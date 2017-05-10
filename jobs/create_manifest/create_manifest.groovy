node{
    withEnv([
        "IS_OFFICIAL_RELEASE=${env.IS_OFFICIAL_RELEASE}",
        "branch=${env.branch}",
        "date=${env.date}",
        "timezone=${env.timezone}",
        "BINTRAY_SUBJECT=${env.BINTRAY_SUBJECT}",
        "BINTRAY_REPO=binary"]){
        deleteDir()
        dir("on-build-config"){
            checkout scm
        }
        withCredentials([
            usernamePassword(credentialsId: 'f966b0fd-a85f-45fe-ac6a-f160aca367e8', 
            passwordVariable: 'BINTRAY_API_KEY', 
            usernameVariable: 'BINTRAY_USERNAME')
            ]){
            sh './on-build-config/jobs/create_manifest/create_manifest.sh'
        }
        // inject properties file as environment variables
        if(fileExists ('downstream_file')) {
            def props = readProperties file: 'downstream_file'
            if(props['MANIFEST_FILE_URL']) {
                env.MANIFEST_FILE_URL = "${props.MANIFEST_FILE_URL}"
                env.manifest_name = "${props.manifest_name}"
            }
            else{
                error("Failed because the manifest file url is not generated")
            }
        }
    }
}

