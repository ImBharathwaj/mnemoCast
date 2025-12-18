#!/bin/bash
# Test script for OOH Ad Serving Engine API Endpoints
# This script tests all available endpoints including OOH features

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
SCREEN_ID="test-screen-001"
AD_ID=""

# Helper functions
print_section() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_test() {
    echo -e "${YELLOW}→ Testing: $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ Success${NC}"
}

print_error() {
    echo -e "${RED}✗ Error: $1${NC}"
}

check_server() {
    print_section "Checking Server Status"
    if curl -s -f "${BASE_URL}/ads/deliver" > /dev/null 2>&1 || [ $? -eq 0 ] || [ $? -eq 52 ] || [ $? -eq 56 ]; then
        print_success "Server is reachable at ${BASE_URL}"
        return 0
    else
        print_error "Server is not reachable at ${BASE_URL}"
        echo ""
        echo "Please start the server first:"
        echo "  cd backend && sbt \"project engineApi\" run"
        echo ""
        echo "Or use the dev script:"
        echo "  ./scripts/dev-run.sh"
        echo ""
        exit 1
    fi
}

# ============================================
# Screen Management Endpoints
# ============================================

test_screen_registration() {
    print_section "1. Screen Registration"
    print_test "POST /api/v1/screens/register"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/screens/register" \
        -H "Content-Type: application/json" \
        -d '{
            "id": "'"${SCREEN_ID}"'",
            "name": "Phoenix Mall - Food Court Screen 1",
            "location": {
                "country": "IN",
                "city": "Chennai",
                "area": "Phoenix Mall",
                "venueType": "mall",
                "timezone": "Asia/Kolkata"
            },
            "tags": ["mall", "food-court", "premium"],
            "metadata": {
                "resolution": "1920x1080",
                "orientation": "landscape"
            }
        }')
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" = "200" ]; then
        print_success
        echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
    else
        print_error "HTTP $HTTP_CODE"
        echo "$BODY"
    fi
}

test_get_screen() {
    print_section "2. Get Screen by ID"
    print_test "GET /api/v1/screens/${SCREEN_ID}"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}/api/v1/screens/${SCREEN_ID}")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" = "200" ]; then
        print_success
        echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
    else
        print_error "HTTP $HTTP_CODE"
        echo "$BODY"
    fi
}

test_list_screens() {
    print_section "3. List All Screens"
    print_test "GET /api/v1/screens"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}/api/v1/screens")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" = "200" ]; then
        print_success
        echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
    else
        print_error "HTTP $HTTP_CODE"
        echo "$BODY"
    fi
}

test_screen_heartbeat() {
    print_section "4. Screen Heartbeat"
    print_test "PUT /api/v1/screens/${SCREEN_ID}/heartbeat"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "${BASE_URL}/api/v1/screens/${SCREEN_ID}/heartbeat")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" = "200" ]; then
        print_success
        echo "$BODY"
    else
        print_error "HTTP $HTTP_CODE"
        echo "$BODY"
    fi
}

# ============================================
# Ad Management Endpoints
# ============================================

test_create_ad() {
    print_section "5. Create Ad with OOH Features"
    print_test "POST /admin/ads"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/admin/ads" \
        -H "Content-Type: application/json" \
        -d '{
            "advertiserId": "brand-x",
            "creativeUrl": "https://cdn.example.com/ad1.mp4",
            "targetUrl": "https://example.com",
            "durationSeconds": 30,
            "targetingRules": [
                {"key": "city", "operator": "eq", "value": "Chennai"},
                {"key": "venueType", "operator": "eq", "value": "mall"}
            ],
            "isActive": true,
            "maxPlays": 1000,
            "dailyLimit": 100,
            "hourlyLimit": 10
        }')
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" = "200" ]; then
        print_success
        echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
        # Extract ad ID for later use
        AD_ID=$(echo "$BODY" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null || echo "")
    else
        print_error "HTTP $HTTP_CODE"
        echo "$BODY"
    fi
}

test_create_ad_with_time_targeting() {
    print_section "6. Create Ad with Time-Based Targeting"
    print_test "POST /admin/ads (with daypart targeting)"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/admin/ads" \
        -H "Content-Type: application/json" \
        -d '{
            "advertiserId": "brand-y",
            "creativeUrl": "https://cdn.example.com/ad2.mp4",
            "targetUrl": "https://example.com",
            "durationSeconds": 15,
            "targetingRules": [
                {"key": "city", "operator": "eq", "value": "Chennai"},
                {"key": "time", "operator": "daypart", "value": "09:00-17:00,monday,friday"}
            ],
            "isActive": true,
            "maxPlays": 500
        }')
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" = "200" ]; then
        print_success
        echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
    else
        print_error "HTTP $HTTP_CODE"
        echo "$BODY"
    fi
}

test_list_ads() {
    print_section "7. List All Ads"
    print_test "GET /admin/ads"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}/admin/ads")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" = "200" ]; then
        print_success
        echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
    else
        print_error "HTTP $HTTP_CODE"
        echo "$BODY"
    fi
}

# ============================================
# Playlist Generation
# ============================================

test_playlist_generation() {
    print_section "8. Playlist Generation"
    print_test "GET /api/v1/screens/${SCREEN_ID}/playlist?durationMinutes=3"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}/api/v1/screens/${SCREEN_ID}/playlist?durationMinutes=3")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" = "200" ]; then
        print_success
        echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
    elif [ "$HTTP_CODE" = "204" ]; then
        echo -e "${YELLOW}No playlist available (no eligible ads)${NC}"
    else
        print_error "HTTP $HTTP_CODE"
        echo "$BODY"
    fi
}

# ============================================
# Ad Delivery (with OOH parameters)
# ============================================

test_ad_delivery_basic() {
    print_section "9. Ad Delivery (Basic)"
    print_test "GET /ads/deliver"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}/ads/deliver")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" = "200" ]; then
        print_success
        echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
    elif [ "$HTTP_CODE" = "204" ]; then
        echo -e "${YELLOW}No ads available${NC}"
    else
        print_error "HTTP $HTTP_CODE"
        echo "$BODY"
    fi
}

test_ad_delivery_ooh() {
    print_section "10. Ad Delivery (with OOH Parameters)"
    print_test "GET /ads/deliver?screenId=${SCREEN_ID}&city=Chennai&venueType=mall&timezone=Asia/Kolkata"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}/ads/deliver?screenId=${SCREEN_ID}&city=Chennai&venueType=mall&timezone=Asia/Kolkata")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" = "200" ]; then
        print_success
        echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
    elif [ "$HTTP_CODE" = "204" ]; then
        echo -e "${YELLOW}No ads available${NC}"
    else
        print_error "HTTP $HTTP_CODE"
        echo "$BODY"
    fi
}

# ============================================
# Main execution
# ============================================

main() {
    echo ""
    echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  OOH Ad Serving Engine API Tester     ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
    echo ""
    echo "Base URL: ${BASE_URL}"
    echo "Screen ID: ${SCREEN_ID}"
    echo ""
    
    # Check if server is running
    check_server
    
    # Run tests
    test_screen_registration
    test_get_screen
    test_list_screens
    test_screen_heartbeat
    
    test_create_ad
    test_create_ad_with_time_targeting
    test_list_ads
    
    test_playlist_generation
    test_ad_delivery_basic
    test_ad_delivery_ooh
    
    print_section "Test Summary"
    echo -e "${GREEN}All tests completed!${NC}"
    echo ""
    echo "Next steps:"
    echo "  - Check the responses above for any errors"
    echo "  - Try generating playlists with different durationMinutes"
    echo "  - Test with different targeting rules"
    echo ""
}

# Run main function
main

