/*
The MIT License (MIT)

Copyright (c) 2014, Groupon, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package com.groupon.jenkins.branchhistory;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.groupon.jenkins.dynamic.build.DbBackedBuild;
import com.groupon.jenkins.dynamic.build.DbBackedProject;
import com.groupon.jenkins.dynamic.build.DynamicProject;
import com.groupon.jenkins.dynamic.build.repository.DynamicBuildRepository;
import hudson.widgets.HistoryWidget;

import java.util.ArrayList;
import java.util.ListIterator;

class BranchHistoryWidgetModel<T extends DbBackedBuild> {

    private final String branch;
    private final DynamicBuildRepository dynamicBuildRepository;
    private final DynamicProject owner;

    public BranchHistoryWidgetModel(DynamicProject owner, DynamicBuildRepository dynamicBuildRepository, String branch) {
        this.owner = owner;
        this.dynamicBuildRepository = dynamicBuildRepository;
        this.branch = branch;
    }

    public Iterable<T> getBuildsAfter(int n) {
        return filterSkipped(isMyBuilds() ? dynamicBuildRepository.<T> getCurrentUserBuildsGreaterThan((DbBackedProject) owner, n) : dynamicBuildRepository.<T> getBuildGreaterThan((DbBackedProject) owner, n, branch));
    }

    public Iterable<T> getBaseList() {
        return filterSkipped(isMyBuilds() ? dynamicBuildRepository.<T> getCurrentUserBuilds(((DbBackedProject) owner), BranchHistoryWidget.BUILD_COUNT) : dynamicBuildRepository.<T> getLast((DbBackedProject) owner, BranchHistoryWidget.BUILD_COUNT, branch));
    }



    private Iterable<T> filterSkipped(Iterable<T> builds) {
        return Iterables.filter(builds, new Predicate<T>() {
            @Override
            public boolean apply(T build) {
                return !build.isSkipped();
            }
        });
    }

    private boolean isMyBuilds() {
        return false;// BranchHistoryWidget.MY_BUILDS_BRANCH.equals(this.branch);
    }



    public Iterable<T> getBuildsInProgress(int firstBuildNumber, int lastBuildNumber) {
        return filterSkipped(dynamicBuildRepository.<T> getBuildsInProgress((DbBackedProject) owner,branch,firstBuildNumber,lastBuildNumber));
    }

}
