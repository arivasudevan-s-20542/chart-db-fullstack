#!/bin/bash

# ChartDB Catalyst Deployment Script
# Usage: ./deploy-catalyst.sh [environment]
# Environments: development (default), production

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ENVIRONMENT="${1:-development}"

echo "╔════════════════════════════════════════════════════════════╗"
echo "║       ChartDB Catalyst Deployment Script                    ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
echo "Environment: $ENVIRONMENT"
echo "Project Root: $PROJECT_ROOT"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_step() {
    echo -e "${GREEN}[STEP]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    print_step "Checking prerequisites..."
    
    # Check Node.js
    if ! command -v node &> /dev/null; then
        print_error "Node.js is not installed. Please install Node.js 20+"
        exit 1
    fi
    
    # Check Java
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed. Please install Java 21+"
        exit 1
    fi
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed. Please install Maven 3.9+"
        exit 1
    fi
    
    # Check Catalyst CLI
    if ! command -v catalyst &> /dev/null; then
        print_warning "Catalyst CLI not found. Installing..."
        npm install -g zcatalyst-cli
    fi
    
    echo "✓ All prerequisites met"
}

# Build backend
build_backend() {
    print_step "Building backend with Java 17..."
    
    cd "$PROJECT_ROOT/backend"
    
    # Use Java 17 if available (for compatibility with Catalyst)
    if [ -d "/opt/homebrew/opt/openjdk@17" ]; then
        export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
        export PATH="$JAVA_HOME/bin:$PATH"
        echo "Using Java 17: $JAVA_HOME"
    elif [ -d "/usr/lib/jvm/java-17-openjdk" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-17-openjdk"
        export PATH="$JAVA_HOME/bin:$PATH"
        echo "Using Java 17: $JAVA_HOME"
    fi
    
    java -version
    mvn clean package -DskipTests
    
    echo "✓ Backend built successfully"
}

# Build frontend
build_frontend() {
    print_step "Building frontend..."
    
    cd "$PROJECT_ROOT/frontend"
    
    # Install dependencies if needed
    if [ ! -d "node_modules" ]; then
        npm ci
    fi
    
    # Build with Catalyst-specific environment
    VITE_API_URL="/server/chartdb_backend/api" \
    VITE_WS_URL="/server/chartdb_backend/ws" \
    npm run build
    
    # Copy to Catalyst client directory
    mkdir -p "$PROJECT_ROOT/catalyst/client"
    # Remove old files but keep client-package.json
    find "$PROJECT_ROOT/catalyst/client" -mindepth 1 ! -name 'client-package.json' -delete
    cp -r dist/* "$PROJECT_ROOT/catalyst/client/"
    
    echo "✓ Frontend built successfully"
}

# Deploy to Catalyst
deploy_to_catalyst() {
    print_step "Deploying to Zoho Catalyst..."
    
    cd "$PROJECT_ROOT/catalyst"
    
    # Check if logged in
    if ! catalyst whoami &> /dev/null; then
        print_warning "Not logged in to Catalyst. Please login..."
        catalyst auth:login
    fi
    
    # Check if project is initialized (look for .catalyst file)
    if [ ! -f ".catalyst" ]; then
        print_warning "Catalyst project not initialized. Initializing..."
        echo ""
        echo "You will need to select your Catalyst project from the list."
        echo "If you haven't created one yet, create it at https://console.catalyst.zoho.com/"
        echo ""
        catalyst init
    fi
    
    # Deploy (AppSail must be created in console first)
    echo ""
    echo "NOTE: If AppSail deployment fails, create it in Catalyst Console first:"
    echo "  1. Go to https://console.catalyst.zoho.com/"
    echo "  2. Select your project → Develop → AppSail"
    echo "  3. Create AppSail: name=chartdb-backend, stack=Java 17, port=8080"
    echo ""
    
    catalyst deploy
    
    echo "✓ Deployed successfully"
}

# Main execution
main() {
    check_prerequisites
    build_backend
    build_frontend
    deploy_to_catalyst
    
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║                 Deployment Complete! 🚀                     ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    echo "Your application is now deployed to Zoho Catalyst."
    echo "Check the Catalyst console for the application URL."
    echo ""
}

main
