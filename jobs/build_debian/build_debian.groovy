node(build_debian_node){
    lock("debian"){
        timestamps{
            withEnv([
                "MANIFEST_FILE_URL=${env.MANIFEST_FILE_URL}",
                "IS_OFFICIAL_RELEASE=${env.IS_OFFICIAL_RELEASE}",
                "BINTRAY_SUBJECT=${env.BINTRAY_SUBJECT}",
                "BINTRAY_REPO=debian",
                "CI_BINTRAY_SUBJECT=${env.CI_BINTRAY_SUBJECT}",
                "CI_BINTRAY_REPO=debian",
                "BINTRAY_COMPONENT=main",
                "BINTRAY_DISTRIBUTION=trusty", 
                "BINTRAY_ARCHITECTURE=amd64"]) {
                deleteDir()
                dir("on-build-config"){
                    checkout scm
                }

                // credentials are binding to Jenkins Server
                withCredentials([
                    usernameColonPassword(credentialsId: "ff7ab8d2-e678-41ef-a46b-dd0e780030e1", 
                                          variable: "SUDO_CREDS"),
                    usernameColonPassword(credentialsId: "f966b0fd-a85f-45fe-ac6a-f160aca367e8", 
                                          variable:"BINTRAY_CREDS")]){
                    sh './on-build-config/jobs/build_debian/build_debian.sh'
                }

                // inject properties file as environment variables
                if(fileExists ("downstream_file")) {
                    def props = readProperties file: "downstream_file"
                    if(props["RACKHD_VERSION"]) {
                        env.RACKHD_VERSION = "${props.RACKHD_VERSION}"
                    }
                    if(props["RACKHD_COMMIT"]) {
                        env.RACKHD_COMMIT = "${props.RACKHD_COMMIT}"
                    }
                }
             
                archiveArtifacts 'b/**/*.deb, downstream_file'
                stash name: 'debians', includes: 'b/**/*.deb'
            }
        }
    }
}
