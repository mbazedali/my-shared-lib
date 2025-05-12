def printParams() {
    stage('Print Params') {
        if (params.PRINT_ENV) {
            echo "Build #${env.BUILD_NUMBER} on branch ${env.GIT_BRANCH}"
            sh 'printenv'
        }
    }
}

//Verify Maven installation
def verifyMaven() {
    stage('Verify Maven') {
        sh 'mvn -v || echo "Maven not executable"'
    }
}

//Build & unit-test (or skip tests)
def buildAndTest() {
    stage('Build & Test') {
        script {
            if (params.RUN_TESTS) {
                sh 'mvn -B clean verify'
            } else {
                sh 'mvn -B -DskipTests clean package'
            }
        }
    }
}

//Integration & Lint
def integrationAndLint() {
    stage('Integration & Lint') {
        sh 'mvn checkstyle:check'
        sh 'mvn verify'
    }
}

//Prepare deployment target
def prepareDeployTarget() {
    stage('Prepare Deploy Target') {
        script {
            env.DEPLOY_TARGET = generateDeploymentName(env.APP_NAME, env.GIT_BRANCH, env.BUILD_NUMBER)
            echo "📦 Deployment target: ${env.DEPLOY_TARGET}"
        }
    }
}

//Deploy to prod (only on master + ENV=prod)
def deploy() {
    if (env.BRANCH_NAME == 'master' && env.ENV == 'prod') {
        stage('Deploy') {
            echo "Deploying ${env.APP_NAME} to ${env.DEPLOY_TARGET}"
            sh "mkdir -p ${env.DEPLOY_DIR}"
            script {
                def pom = readMavenPom file: 'pom.xml'
                def jarFile = "target/${pom.artifactId}-${pom.version}.jar"
                if (fileExists("${jarFile}")) {
                    sh "cp ${jarFile} ${env.DEPLOY_TARGET}"
                } else {
                    error "Deployment failed, ${jarFile} does not exist."
                }
            }
        }
    } else {
        echo "Skipping deployment as the conditions were not met."
    }
}


//Emit changelog
def changelog() {
    stage('Changelog') {
        script {
            def changes = currentBuild.changeSets
                             .collect { cs -> cs.items*.msg }
                             .flatten()
                             .join("\n")
            echo "Changes in this build:\n${changes ?: '— no changes'}"
        }
    }
}

// Expose functions
// return this
