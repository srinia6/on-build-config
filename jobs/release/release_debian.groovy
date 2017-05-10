node(build_debian_node){
    withEnv([
        "BINTRAY_SUBJECT=${env.BINTRAY_SUBJECT}",
        "BINTRAY_REPO=debian",
        "BINTRAY_COMPONENT=main", 
        "BINTRAY_DISTRIBUTION=trusty", 
        "BINTRAY_ARCHITECTURE=amd64"]){
        deleteDir()
        dir("on-build-config"){
            checkout scm
        }
        dir("DEBIAN"){
            unstash "debians"
        }
        withCredentials([
            usernameColonPassword(credentialsId: 'f966b0fd-a85f-45fe-ac6a-f160aca367e8', 
                                  variable: 'BINTRAY_CREDS')]){
            sh './on-build-config/jobs/release/release_debian.sh'
        }
    }
}

