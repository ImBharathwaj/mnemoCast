#!/bin/bash
# Comprehensive Seed Script for Mnemocast Demo
# Creates realistic demo data: screens, campaigns, creatives

set -e

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
echo "🌱 Seeding comprehensive demo data to $API_BASE_URL"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper function to make API calls
api_call() {
  local method=$1
  local endpoint=$2
  local data=$3
  
  if [ -z "$data" ]; then
    curl -s -X "$method" "$API_BASE_URL$endpoint" \
      -H "Content-Type: application/json"
  else
    curl -s -X "$method" "$API_BASE_URL$endpoint" \
      -H "Content-Type: application/json" \
      -d "$data"
  fi
}

echo -e "${YELLOW}📺 Creating Screens...${NC}"

# Chennai Screens
api_call POST "/api/v1/screens/register" '{
  "screenId": "screen-chennai-mall-1",
  "name": "Phoenix Mall Chennai - Main Entrance",
  "location": {"city": "Chennai", "area": "Velachery"},
  "classification": 7,
  "tags": ["mall", "premium", "retail"]
}'

api_call POST "/api/v1/screens/register" '{
  "screenId": "screen-chennai-airport-1",
  "name": "Chennai Airport - Terminal 1",
  "location": {"city": "Chennai", "area": "Meenambakkam"},
  "classification": 8,
  "tags": ["airport", "transit", "premium"]
}'

api_call POST "/api/v1/screens/register" '{
  "screenId": "screen-chennai-transit-1",
  "name": "Chennai Metro - Teynampet Station",
  "location": {"city": "Chennai", "area": "Teynampet"},
  "classification": 4,
  "tags": ["transit", "metro", "commuter"]
}'

# Mumbai Screens
api_call POST "/api/v1/screens/register" '{
  "screenId": "screen-mumbai-mall-1",
  "name": "Phoenix Marketcity Mumbai - Food Court",
  "location": {"city": "Mumbai", "area": "Kurla"},
  "classification": 6,
  "tags": ["mall", "retail", "food"]
}'

api_call POST "/api/v1/screens/register" '{
  "screenId": "screen-mumbai-airport-1",
  "name": "Mumbai Airport - T2 International",
  "location": {"city": "Mumbai", "area": "Andheri"},
  "classification": 8,
  "tags": ["airport", "premium", "international"]
}'

api_call POST "/api/v1/screens/register" '{
  "screenId": "screen-mumbai-office-1",
  "name": "Bandra Kurla Complex - Tower A",
  "location": {"city": "Mumbai", "area": "BKC"},
  "classification": 5,
  "tags": ["office", "corporate", "premium"]
}'

# Bangalore Screens
api_call POST "/api/v1/screens/register" '{
  "screenId": "screen-bangalore-mall-1",
  "name": "Orion Mall Bangalore - Cinema Hall",
  "location": {"city": "Bangalore", "area": "Rajajinagar"},
  "classification": 7,
  "tags": ["mall", "entertainment", "premium"]
}'

api_call POST "/api/v1/screens/register" '{
  "screenId": "screen-bangalore-tech-1",
  "name": "ITPL Whitefield - Tech Park Lobby",
  "location": {"city": "Bangalore", "area": "Whitefield"},
  "classification": 6,
  "tags": ["office", "tech", "corporate"]
}'

api_call POST "/api/v1/screens/register" '{
  "screenId": "screen-bangalore-transit-1",
  "name": "Bangalore Metro - MG Road Station",
  "location": {"city": "Bangalore", "area": "MG Road"},
  "classification": 5,
  "tags": ["transit", "metro", "commuter"]
}'

echo -e "${GREEN}✅ Created 9 screens${NC}"

echo -e "${YELLOW}📢 Creating Campaigns...${NC}"

# Campaign 1: Premium Fashion Brand
CAMPAIGN1=$(api_call POST "/api/v1/campaigns" '{
  "name": "Luxury Fashion Summer 2024",
  "status": "active",
  "priority": 9,
  "startDate": "2024-01-01T00:00:00Z",
  "endDate": "2024-12-31T23:59:59Z",
  "totalBudget": 50000,
  "targeting": {
    "tags": ["premium", "mall"],
    "cities": ["Chennai", "Mumbai", "Bangalore"],
    "minClassification": 6
  }
}')

CAMPAIGN1_ID=$(echo $CAMPAIGN1 | grep -o '"id":"[^"]*' | cut -d'"' -f4)
echo "Created campaign: $CAMPAIGN1_ID"

# Campaign 2: Tech Product Launch
CAMPAIGN2=$(api_call POST "/api/v1/campaigns" '{
  "name": "Tech Gadget Launch Q1 2024",
  "status": "active",
  "priority": 8,
  "startDate": "2024-01-01T00:00:00Z",
  "endDate": "2024-03-31T23:59:59Z",
  "totalBudget": 30000,
  "targeting": {
    "tags": ["tech", "office", "corporate"],
    "cities": ["Bangalore", "Mumbai"],
    "minClassification": 5
  }
}')

CAMPAIGN2_ID=$(echo $CAMPAIGN2 | grep -o '"id":"[^"]*' | cut -d'"' -f4)
echo "Created campaign: $CAMPAIGN2_ID"

# Campaign 3: Travel & Tourism
CAMPAIGN3=$(api_call POST "/api/v1/campaigns" '{
  "name": "Travel Deals Summer 2024",
  "status": "active",
  "priority": 7,
  "startDate": "2024-01-01T00:00:00Z",
  "endDate": "2024-06-30T23:59:59Z",
  "totalBudget": 40000,
  "targeting": {
    "tags": ["airport", "transit"],
    "cities": ["Chennai", "Mumbai"],
    "minClassification": 4
  }
}')

CAMPAIGN3_ID=$(echo $CAMPAIGN3 | grep -o '"id":"[^"]*' | cut -d'"' -f4)
echo "Created campaign: $CAMPAIGN3_ID"

# Campaign 4: Food & Beverage
CAMPAIGN4=$(api_call POST "/api/v1/campaigns" '{
  "name": "Restaurant Chain Promotion",
  "status": "active",
  "priority": 6,
  "startDate": "2024-01-01T00:00:00Z",
  "endDate": "2024-12-31T23:59:59Z",
  "totalBudget": 25000,
  "targeting": {
    "tags": ["mall", "food"],
    "cities": ["Chennai", "Mumbai", "Bangalore"],
    "minClassification": 4
  }
}')

CAMPAIGN4_ID=$(echo $CAMPAIGN4 | grep -o '"id":"[^"]*' | cut -d'"' -f4)
echo "Created campaign: $CAMPAIGN4_ID"

# Campaign 5: Local Services
CAMPAIGN5=$(api_call POST "/api/v1/campaigns" '{
  "name": "Local Services Metro Campaign",
  "status": "active",
  "priority": 5,
  "startDate": "2024-01-01T00:00:00Z",
  "endDate": "2024-12-31T23:59:59Z",
  "totalBudget": 15000,
  "targeting": {
    "tags": ["transit", "metro"],
    "cities": ["Chennai", "Bangalore"],
    "minClassification": 3
  }
}')

CAMPAIGN5_ID=$(echo $CAMPAIGN5 | grep -o '"id":"[^"]*' | cut -d'"' -f4)
echo "Created campaign: $CAMPAIGN5_ID"

echo -e "${GREEN}✅ Created 5 campaigns${NC}"

echo -e "${YELLOW}🎨 Creating Creatives...${NC}"

# Creatives for Campaign 1 (Fashion)
for i in {1..4}; do
  api_call POST "/api/v1/creatives" "{
    \"campaignId\": \"$CAMPAIGN1_ID\",
    \"name\": \"Fashion Creative $i\",
    \"mediaUrl\": \"https://example.com/creatives/fashion-$i.mp4\",
    \"durationSeconds\": 15,
    \"status\": \"active\",
    \"weight\": $((i * 2))
  }"
done

# Creatives for Campaign 2 (Tech)
for i in {1..3}; do
  api_call POST "/api/v1/creatives" "{
    \"campaignId\": \"$CAMPAIGN2_ID\",
    \"name\": \"Tech Creative $i\",
    \"mediaUrl\": \"https://example.com/creatives/tech-$i.mp4\",
    \"durationSeconds\": 20,
    \"status\": \"active\",
    \"weight\": $((i * 3))
  }"
done

# Creatives for Campaign 3 (Travel)
for i in {1..3}; do
  api_call POST "/api/v1/creatives" "{
    \"campaignId\": \"$CAMPAIGN3_ID\",
    \"name\": \"Travel Creative $i\",
    \"mediaUrl\": \"https://example.com/creatives/travel-$i.mp4\",
    \"durationSeconds\": 30,
    \"status\": \"active\",
    \"weight\": $((i * 2))
  }"
done

# Creatives for Campaign 4 (Food)
for i in {1..4}; do
  api_call POST "/api/v1/creatives" "{
    \"campaignId\": \"$CAMPAIGN4_ID\",
    \"name\": \"Food Creative $i\",
    \"mediaUrl\": \"https://example.com/creatives/food-$i.mp4\",
    \"durationSeconds\": 15,
    \"status\": \"active\",
    \"weight\": $((i * 2))
  }"
done

# Creatives for Campaign 5 (Local Services)
for i in {1..3}; do
  api_call POST "/api/v1/creatives" "{
    \"campaignId\": \"$CAMPAIGN5_ID\",
    \"name\": \"Local Services Creative $i\",
    \"mediaUrl\": \"https://example.com/creatives/local-$i.mp4\",
    \"durationSeconds\": 10,
    \"status\": \"active\",
    \"weight\": $i
  }"
done

echo -e "${GREEN}✅ Created 17 creatives${NC}"

echo -e "${GREEN}🎉 Demo data seeding complete!${NC}"
echo ""
echo "Summary:"
echo "  - 9 Screens (Chennai, Mumbai, Bangalore)"
echo "  - 5 Campaigns (various targeting strategies)"
echo "  - 17 Creatives (distributed across campaigns)"
echo ""
echo "You can now:"
echo "  1. View campaigns in the dashboard"
echo "  2. Check analytics for performance metrics"
echo "  3. Test playlist generation for different screens"
echo "  4. Monitor system health and metrics"

