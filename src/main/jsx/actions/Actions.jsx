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
import { Actions } from 'flummox';
import {addBranchTab as addBranchTabOnServer, removeBranchTab as removeBranchTabOnServer,
  build,
  cancelBuild as cancelBuildApi,
  buildLog ,
  recentProjects,
  job,
  deleteCurrentProject, 
  fetchBuildHistory} from '../api/Api.jsx';
  import babel_polyfill from 'babel/polyfill';
  export default class AppActions extends Actions {
    currentBuildChanged(buildNumber,subBuild){
      this.jobInfoChanged({build:null});
      this.refreshBuild(buildNumber,subBuild);
    }
    async loadBuildLog(buildNumber,subBuild){
      this.jobInfoChanged({build: { log:null }});
      const logText = await buildLog(buildNumber,subBuild);
      this.jobInfoChanged({build: { log:logText.split("\n") }});
    }
    refreshBuild(buildNumber,subBuild){
      build(buildNumber).then( build => {
        buildLog(buildNumber,subBuild).then(logText => {
          build['log'] = logText.split("\n");
          this.jobInfoChanged({build});
        })
      });
    }
    jobInfoChanged(newJobInfo){
      return newJobInfo;
    }
    clearJobInfo(path){
      return path;
    }
    async cancelBuild(url){
      cancelBuildApi(url);
    }

    async getRecentProjectsFromServer(){
      let  projects = await recentProjects();
      this.recentProjectsChanged(projects.recentProjects);
    }
    getJobInfoFromServer(tree,branchTab,buildCount){
      job(tree,branchTab,buildCount).then(jobInfo => this.jobInfoChanged(jobInfo));
    }

    recentProjectsChanged(recentProjects) {
      return recentProjects;
    }

    async buildHistorySelected(branch,count){
      let  builds = await fetchBuildHistory(branch,count);
      this.buildHistoryChanged(builds.builds);
    }

    buildHistoryChanged(builds){
      return builds;
    }
    async removeBranchTab(tab){
      await removeBranchTabOnServer(tab);
      this.tabRemoved(tab);
    }
    tabRemoved(tab){
      return tab;
    }
    async addBranchTab(tabRegex){
      await addBranchTabOnServer(tabRegex);
      this.tabAdded(tabRegex);
    }
    tabAdded(tabRegex){
      return tabRegex;
    }
  }
