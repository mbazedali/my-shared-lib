def call(String appName, String branchName, String buildNumber, String deployDir = '/opt/app_deployments'){
    def safeBranch = branchName.replaceAll('/','-')
    return \"${deployDir}/${appName}-${safeBranch}-${buildNumber}.jar"
}