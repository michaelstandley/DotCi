/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014, Groupon, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.groupon.jenkins.buildtype.docker;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.groupon.jenkins.buildtype.dockerimage.DockerCommandBuilder;
import com.groupon.jenkins.buildtype.plugins.DotCiPluginAdapter;
import com.groupon.jenkins.buildtype.util.config.Config;
import com.groupon.jenkins.buildtype.util.shell.ShellCommands;
import com.groupon.jenkins.extensions.DotCiExtensionsHelper;
import com.groupon.jenkins.notifications.PostBuildNotifier;
import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.Combination;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import static com.groupon.jenkins.buildtype.dockerimage.DockerCommandBuilder.dockerCommand;

public abstract class DockerBuildConfiguration {
    protected Config config;
    protected String buildId;
    protected ShellCommands checkoutCommands;
    protected Stack<String> linkCleanupCommands = new Stack<String>();

    public DockerBuildConfiguration(Config config, String buildId, ShellCommands checkoutCommands) {
        this.config = config;
        this.buildId = buildId;
        this.checkoutCommands = checkoutCommands;
    }
    public AxisList getAxisList() {
        AxisList  axisList = new AxisList(new Axis("command", "main"));
        if (isParallized()) {
            Set commandKeys =  ((Map) config.get("command")).keySet();
            axisList = new AxisList(new Axis("command", new ArrayList<String>(commandKeys)));
        }
        return axisList;
    }

    public boolean isParallized() {
        return config.get("command") instanceof Map;
    }
    public Stack<String> getLinkCleanupCommands() {
        return linkCleanupCommands;
    }

    public abstract ShellCommands toShellCommands(Combination combination);

    protected List<String> linkServicesToRunCommand(DockerCommandBuilder dockerRunCommand, List<Map<String, Object>> links) {
        List<String> commands = new ArrayList<String>();
        if(links != null){
            commands.addAll(getStartLinkedImageCommands(links));
            linkServices(dockerRunCommand,links);
            linkCleanupCommands.addAll(getLinksCleanupCommands(links));
        }
        commands.add(dockerRunCommand.get());
        return commands;

    }

    private void linkServices(DockerCommandBuilder dockerRunCommand, List<Map<String,Object>> links) {
        for (String link : getContainerLinkCommands(links)) {
            dockerRunCommand.flag("link", link);
        }
    }

    private List<String> getStartLinkedImageCommands(List<Map<String, Object>> links) {
        List<String> startLinkCommands = new ArrayList<String>();
            for (Map<String,Object> link: links){
                String linkImageName = (String) link.get("image");
                 DockerCommandBuilder runCommand = DockerCommandBuilder.dockerCommand("run")
                        .flag("d")
                        .flag("name", getContainerId(linkImageName))
                        .bulkOptions((String) link.get("run_params"));
                runCommand =  link.containsKey("command")? runCommand.args(linkImageName, "sh -cx \"" + link.get("command")+ "\""): runCommand.args(linkImageName);
                List<String> runCommands = linkServicesToRunCommand(runCommand, (List<Map<String, Object>>) link.get("links"));
                startLinkCommands.addAll(runCommands);
            }
        return startLinkCommands;
    }

    protected String getContainerId(String serviceImageName) {
        String serviceId = serviceImageName.replaceAll("/", "_").replaceAll(":", "_").replaceAll("\\.", "_");
        return serviceId + "_" + buildId;
    }

    public Iterable<String> getContainerLinkCommands( List<Map<String,Object>> links) {

        return Iterables.transform(links, new Function<Map<String, Object>, String>() {
            @Override
            public String apply(Map<String, Object> link) {
                String serviceImageName = (String) link.get("image");
                String serviceId = link.containsKey("name") ? (String) link.get("name") : getServiceRuntimeId(serviceImageName);
                String runningImageId = getContainerId(serviceImageName);
                return runningImageId + ":" + serviceId;
            }
        });

    }

    private String getServiceRuntimeId(String serviceImageName) {
        String[] serviceParts = serviceImageName.split("/");
        return serviceParts[serviceParts.length - 1].split(":")[0];
    }

    private List<String> getLinksCleanupCommands(List<Map<String, Object>> links) {
        List<String> cleanUpCommands = new ArrayList<String>();
        for (String serviceImageName : getServiceImageNames(links)) {
            String killCommand = dockerCommand("kill").args(getContainerId(serviceImageName)).get();
            String removeCommand = dockerCommand("rm").args(getContainerId(serviceImageName)).get();
            cleanUpCommands.add(removeCommand);
            cleanUpCommands.add(killCommand);
        }
        return cleanUpCommands;
    }

    private Iterable<String> getServiceImageNames( List<Map<String,Object>> links) {
        return Iterables.transform(links,new Function<Map<String, Object>, String>() {
            @Override
            public String apply(Map<String, Object> service) {
                return (String) service.get("image");
            }
        });
    }

    protected String getRunCommand(Combination combination) {
        List commands = getCommandForCombination(combination);
         return new ShellCommands().add(checkoutCommands).add(new ShellCommands(commands)).toSingleShellCommand();
    }

    protected List getCommandForCombination(Combination combination) {
        List commands;
        if(isParallized()){
            Map command = config.get("command", Map.class);
            Object scriptCommands= command.get(combination.get("command"));
            commands = scriptCommands instanceof List? (List)scriptCommands: Arrays.asList(scriptCommands);
        }else{
            commands = config.get("command", List.class);
        }
        return commands;
    }

    protected String getImageName() {
        return config.get("image",String.class);
    }

    public List<DotCiPluginAdapter> getPlugins() {
        List plugins = config.get("plugins") != null? (List) config.get("plugins") : Collections.emptyList();
        return new DotCiExtensionsHelper().createPlugins(plugins) ;
    }

    public List<PostBuildNotifier> getNotifiers() {
        List notifiers = config.get("notifications") !=null? (List) config.get("notifications") :Collections.emptyList();
        return new DotCiExtensionsHelper().createNotifiers(notifiers) ;
    }
}
