def printParams() {
    if (params.PRINT_ENV) {
        echo "Build #${env.BUILD_NUMBER} on branch ${env.GIT_BRANCH}"
        sh 'printenv'
    }
}

def verifyMaven() {
    sh 'mvn -v || echo "Maven not executable"'
}

def buildAndTest() {
    if (params.RUN_TESTS) {
        sh 'mvn -B clean verify'
    } else {
        sh 'mvn -B -DskipTests clean package'
    }
}

def integrationAndLint() {
    sh 'mvn checkstyle:check'
    sh 'mvn verify'
}

def prepareDeployTarget() {
    env.DEPLOY_TARGET = generateDeploymentName(env.APP_NAME, env.GIT_BRANCH, env.BUILD_NUMBER)
    echo "📦 Deployment target: ${env.DEPLOY_TARGET}"
}

def deploy() {
    if (env.BRANCH_NAME == 'master' && env.ENV == 'prod') {
        echo "Deploying ${env.APP_NAME} to ${env.DEPLOY_TARGET}"
        sh "mkdir -p ${env.DEPLOY_DIR}"
        def pom = readMavenPom file: 'pom.xml'
        def jarFile = "target/${pom.artifactId}-${pom.version}.jar"
        if (fileExists("${jarFile}")) {
            sh "cp ${jarFile} ${env.DEPLOY_TARGET}"
        } else {
            error "Deployment failed, ${jarFile} does not exist."
        }
    } else {
        echo "Skipping deployment as the conditions were not met."
    }
}

def changelog() {
    def changes = currentBuild.changeSets
                     .collect { cs -> cs.items*.msg }
                     .flatten()
                     .join("\n")
    echo "Changes in this build:\n${changes ?: '— no changes'}"
}
