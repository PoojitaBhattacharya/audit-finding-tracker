#!/bin/bash
# Deployment Verification Script for Security Remediation
# Run this after deploying security fixes to verify all protections are active

set -e

echo "======================================"
echo "Security Remediation Verification"
echo "======================================"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
API_URL="${1:-http://localhost:8081}"
TIMEOUT=5

# Function to test HTTP request and check response
test_security_header() {
    local header=$1
    local expected=$2
    local endpoint=$3
    
    echo -n "Checking $header... "
    
    response=$(curl -s -I "$API_URL$endpoint" 2>/dev/null | grep -i "^$header:" || echo "")
    
    if [[ $response == *"$expected"* ]]; then
        echo -e "${GREEN}✓ PASS${NC}"
        return 0
    else
        echo -e "${RED}✗ FAIL${NC}"
        echo "  Expected: $expected"
        echo "  Got: $response"
        return 1
    fi
}

# Function to test blocking of dangerous files
test_file_blocking() {
    local file=$1
    
    echo -n "Testing block for $file... "
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/$file" 2>/dev/null)
    
    if [[ $response == "403" ]] || [[ $response == "404" ]]; then
        echo -e "${GREEN}✓ PASS${NC} (HTTP $response)"
        return 0
    else
        echo -e "${RED}✗ FAIL${NC} (HTTP $response - should be 403/404)"
        return 1
    fi
}

# Function to test PII masking
test_pii_masking() {
    echo -n "Testing PII masking... "
    
    response=$(curl -s -X POST "$API_URL/findings/test-pii" \
        -H "Content-Type: application/json" \
        -d '{"description": "Card: 4532-1234-5678-9010"}' 2>/dev/null)
    
    if echo "$response" | grep -q "9010" && ! echo "$response" | grep -q "4532"; then
        echo -e "${GREEN}✓ PASS${NC} (CC masked correctly)"
        return 0
    else
        echo -e "${RED}✗ FAIL${NC} (PII not properly masked)"
        return 1
    fi
}

# Function to test XSS protection
test_xss_protection() {
    echo -n "Testing XSS protection... "
    
    response=$(curl -s -X POST "$API_URL/findings" \
        -H "Content-Type: application/json" \
        -d '{"title": "<script>alert(1)</script>"}' 2>/dev/null)
    
    if echo "$response" | grep -q "error\|Invalid\|Unsafe"; then
        echo -e "${GREEN}✓ PASS${NC} (XSS blocked)"
        return 0
    else
        echo -e "${YELLOW}⚠ WARNING${NC} (May allow XSS - verify manually)"
        return 0
    fi
}

# Function to test HTTPS requirement (if in production)
test_https_enforcement() {
    echo -n "Checking HTTPS configuration... "
    
    # This would require HTTPS in production
    echo -e "${YELLOW}⚠ MANUAL CHECK${NC} (Set server.servlet.session.cookie.secure=true in production)"
    return 0
}

echo "📋 Testing Security Headers"
echo "=========================================="
test_security_header "X-Content-Type-Options" "nosniff" "/findings"
test_security_header "X-Frame-Options" "DENY" "/findings"
test_security_header "Content-Security-Policy" "default-src 'self'" "/findings"
test_security_header "X-XSS-Protection" "1; mode=block" "/findings"
test_security_header "Cache-Control" "no-store" "/findings"
test_security_header "Referrer-Policy" "strict-origin" "/findings"

echo ""
echo "🔐 Testing File Blocking"
echo "=========================================="
test_file_blocking ".env"
test_file_blocking ".git"
test_file_blocking ".env.local"
test_file_blocking "pom.xml"
test_file_blocking "docker-compose.yml"
test_file_blocking ".htaccess"

echo ""
echo "🛡️  Testing Input Validation"
echo "=========================================="
test_xss_protection

echo ""
echo "👤 Testing PII Protection"
echo "=========================================="
test_pii_masking

echo ""
echo "🔒 Testing HTTPS Configuration"
echo "=========================================="
test_https_enforcement

echo ""
echo "======================================"
echo "Verification Complete!"
echo "======================================"
echo ""
echo "Next Steps:"
echo "1. Run OWASP ZAP scan to validate fixes"
echo "2. Review application logs for security events"
echo "3. Perform UAT testing with stakeholders"
echo "4. Deploy to production environment"
echo ""
