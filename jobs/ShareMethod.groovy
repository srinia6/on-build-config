def checkout(String url, String branch, String targetDir){
    checkout(
    [$class: 'GitSCM', branches: [[name: branch]],
    extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: targetDir]],
    userRemoteConfigs: [[url: url]]])
}
def checkout(String url, String branch){
    checkout(
    [$class: 'GitSCM', branches: [[name: branch]],
    userRemoteConfigs: [[url: url]]])
}

def checkout(String url){
    checkout(url, "master")
}

def getLockedResourceName(String label_name){
    // Get the resource name whose label contains the parameter label_name
    // The locked resources of the build
    def resources=org.jenkins.plugins.lockableresources.LockableResourcesManager.class.get().getResourcesFromBuild(currentBuild.getRawBuild())
    def resources_name=[]
    for(int i=0;i<resources.size();i++){
        String labels = resources[i].getLabels();
        List label_names = Arrays.asList(labels.split("\\s+"));
        for(int j=0;j<label_names.size();j++){
            if(label_names[j]==label_name){
                resources_name.add(resources[i].getName());
            }
        }
    }
    return resources_name
}

def occupyAvailableLockedResource(String label_name, ArrayList<String> used_resources){
     // The locked resources whose label contains the parameter label_name
    resources = getLockedResourceName(label_name)
    def available_resources = resources - used_resources
    if(available_resources.size > 0){
        used_resources.add(available_resources[0])
        String resource_name = available_resources[0]
        return resource_name
    }
    else{
        error("There is no available resources for $label_name")
    }
}

def buildPackage(String repo_dir){
    // retry times for package build to avoid failing caused by network
    int retry_times = 3
    stage("Packages Build"){
        retry(retry_times){
            load(repo_dir + "/jobs/build_debian/build_debian.groovy")
        }
    }
}

def buildImages(String repo_dir){
    // retry times for images build to avoid failing caused by network
    int retry_times = 3
    stage("Images Build"){
        parallel 'vagrant build':{
            retry(retry_times){
                load(repo_dir + "/jobs/build_vagrant/build_vagrant.groovy")
            }
        }, 'ova build':{
            retry(retry_times){
                load(repo_dir + "/jobs/build_ova/build_ova.groovy")
            }
        }, 'build docker':{
            retry(retry_times){
                load(repo_dir + "/jobs/build_docker/build_docker.groovy")
            }
        }
    }

    stage("Post Test"){
        parallel 'vagrant post test':{
            load(repo_dir + "/jobs/build_vagrant/vagrant_post_test.groovy")
        }, 'ova post test loader':{
            load(repo_dir + "/jobs/build_ova/ova_post_test.groovy")
        }, 'docker post test loader':{
            load(repo_dir + "/jobs/build_docker/docker_post_test.groovy")
        }
    }
}

def publishImages(String repo_dir){
    stage("Publish"){
        parallel 'Publish Debian':{
            load(repo_dir + "/jobs/release/release_debian.groovy")
        }, 'Publish Vagrant':{
            load(repo_dir + "/jobs/release/release_vagrant.groovy")
        }, 'Publish Docker':{
            load(repo_dir + "/jobs/release/release_docker.groovy")
        }, 'Publish NPM':{
            load(repo_dir + "/jobs/release/release_npm.groovy")
        }
    }
}

def createTag(String repo_dir){
    stage("Create Tag"){
        load(repo_dir + "/jobs/SprintRelease/create_tag.groovy")
    }
}

def buildAndPublish(Boolean publish, Boolean tag, String repo_dir){
    buildPackage(repo_dir)

    //buildImages(repo_dir)

    //if(tag){
    //    createTag(repo_dir)
    //}
    //if(publish){
    //    publishImages(repo_dir)
    //}
}

def sendResult(boolean sendJenkinsBuildResults, boolean sendTestResults){
    stage("Send Test Result"){
        try{
            if ("${currentBuild.result}" == null || "${currentBuild.result}" == "null"){
                currentBuild.result = "SUCCESS"
            }
            step([$class: 'VTestResultsAnalyzerStep', sendJenkinsBuildResults: sendJenkinsBuildResults, sendTestResults: sendTestResults])
        } catch(error){
            echo "Caught: ${error}"
        }
    }
}

def downloadManifest(String url, String target){
    withCredentials([
            usernamePassword(credentialsId: 'f966b0fd-a85f-45fe-ac6a-f160aca367e8',
            passwordVariable: 'BINTRAY_API_KEY',
            usernameVariable: 'BINTRAY_USERNAME')
    ]){
        sh 'curl --user $BINTRAY_USERNAME:$BINTRAY_API_KEY --retry 5 --retry-delay 5 ' + "$url" + ' -o ' + "${target}"
    }
}

return this
