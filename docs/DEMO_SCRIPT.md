# 🎬 Mnemocast Demo Script

**Duration:** 5-7 minutes  
**Audience:** Investors, potential clients, technical evaluators  
**Goal:** Demonstrate enterprise-ready OOH ad serving platform with sophisticated targeting, analytics, and monitoring

---

## 📖 Story Narrative

**Scenario:** You're a digital advertising agency managing campaigns for multiple clients across India. You need to:
- Target specific audiences based on location, venue type, and screen quality
- Monitor campaign performance in real-time
- Optimize budget allocation based on performance data
- Ensure system reliability and uptime

**Mnemocast Solution:** A sophisticated OOH ad serving platform that intelligently matches ads to screens based on targeting rules, manages budgets, and provides comprehensive analytics.

---

## 🎯 Demo Flow

### **1. System Overview & Health (1 minute)**

**Talking Points:**
- "Let me start by showing you our system health dashboard"
- "We have real-time monitoring of all system components"
- "Notice the uptime, request rates, and component health"

**Actions:**
1. Navigate to **Monitoring** page
2. Show system health status (should be UP)
3. Point out:
   - System uptime
   - Component health (Redis, Postgres, MinIO)
   - Request metrics (total requests, response times)
   - Error rates

**Key Highlight:** "Enterprise-grade monitoring ensures we can detect and respond to issues immediately"

---

### **2. Campaign Management (1.5 minutes)**

**Talking Points:**
- "Our platform supports sophisticated targeting rules"
- "Campaigns can target by location, venue type, and screen classification"
- "Budget management ensures campaigns don't overspend"

**Actions:**
1. Navigate to **Campaigns** page
2. Show existing campaigns:
   - "Luxury Fashion Summer 2024" - Premium malls, high classification
   - "Tech Gadget Launch" - Corporate offices, tech parks
   - "Travel Deals" - Airports and transit hubs
3. Click on one campaign to show:
   - Targeting rules (cities, tags, classification)
   - Budget allocation and spending
   - Associated creatives

**Key Highlight:** "Multi-dimensional targeting ensures ads reach the right audience at the right place"

---

### **3. Screen Management & Targeting (1 minute)**

**Talking Points:**
- "Screens are registered with location and classification data"
- "Classification (1-8) determines screen quality and premium pricing"
- "Tags enable flexible targeting (mall, airport, transit, premium)"

**Actions:**
1. Navigate to **Screens** page
2. Show screens across different cities:
   - Chennai: Mall, Airport, Metro
   - Mumbai: Mall, Airport, Office
   - Bangalore: Mall, Tech Park, Metro
3. Point out:
   - Location data (city, area)
   - Classification levels
   - Tags for targeting

**Key Highlight:** "Geographic and venue-based targeting enables precise audience reach"

---

### **4. Analytics & Performance (2 minutes)**

**Talking Points:**
- "Real-time analytics provide actionable insights"
- "We track performance at multiple levels: campaign, creative, screen, and geographic"
- "ROI metrics help optimize budget allocation"

**Actions:**
1. Navigate to **Analytics** page
2. Show **Overview** tab:
   - Total impressions
   - Top performing campaigns
   - Recent activity
3. Show **ROI Metrics** tab:
   - Budget utilization per campaign
   - Impressions vs. allocated budget
   - Budget utilization percentages
4. Show **Screen Performance** tab:
   - Top performing screens
   - Performance by location
   - Classification impact
5. Show **Geographic** tab:
   - Performance by city/area
   - Screen distribution
6. Show **Time Series** tab:
   - Impressions over time
   - Trends and patterns

**Key Highlight:** "Multi-dimensional analytics enable data-driven campaign optimization"

---

### **5. Real-time Playlist Generation (1 minute)**

**Talking Points:**
- "Playlists are generated dynamically based on targeting rules"
- "Weight-based selection ensures fair distribution"
- "Budget and frequency caps prevent oversaturation"

**Actions:**
1. Navigate to **Screens** page
2. Select a screen (e.g., "Phoenix Mall Chennai")
3. Click "Get Playlist" or use API:
   ```
   GET /api/v1/screens/{screenId}/playlist?durationMinutes=3
   ```
4. Show playlist response:
   - Creatives from matching campaigns
   - Weight-based distribution
   - Duration calculations

**Key Highlight:** "Dynamic playlist generation ensures relevant ads are always served"

---

### **6. System Monitoring & Reliability (0.5 minutes)**

**Talking Points:**
- "Enterprise-grade monitoring ensures system reliability"
- "Health checks enable Kubernetes-ready deployments"
- "Metrics tracking provides operational insights"

**Actions:**
1. Return to **Monitoring** page
2. Show:
   - Component health status
   - Request/response metrics
   - Error tracking
3. Mention:
   - Kubernetes readiness/liveness probes
   - Auto-scaling capabilities
   - Performance benchmarks

**Key Highlight:** "Production-ready infrastructure with comprehensive monitoring"

---

## 🎤 Closing Statement

"**Mnemocast** provides an enterprise-ready OOH ad serving platform that combines:
- **Sophisticated targeting** for precise audience reach
- **Real-time analytics** for data-driven optimization
- **Enterprise-grade monitoring** for reliable operations
- **Scalable architecture** ready for production deployment

Whether you're managing campaigns for a single brand or multiple clients, Mnemocast gives you the tools to maximize ROI and ensure reliable ad delivery."

---

## 📊 Expected Outcomes

After the demo, the audience should understand:

1. ✅ **Targeting Sophistication:** Multi-dimensional targeting (location, venue, classification)
2. ✅ **Analytics Depth:** Comprehensive performance metrics at all levels
3. ✅ **System Reliability:** Enterprise-grade monitoring and health checks
4. ✅ **Production Readiness:** Scalable, monitored, and documented platform
5. ✅ **Business Value:** ROI tracking, budget management, performance optimization

---

## 🔑 Key Talking Points Summary

- **Targeting:** "Multi-dimensional targeting ensures ads reach the right audience"
- **Analytics:** "Real-time analytics enable data-driven campaign optimization"
- **Reliability:** "Enterprise-grade monitoring ensures 99.9% uptime"
- **Scalability:** "Kubernetes-ready architecture scales with demand"
- **ROI:** "Budget tracking and ROI metrics maximize campaign effectiveness"

---

## 🛠️ Technical Highlights (if asked)

- **Backend:** Scala + Pekko HTTP (high-performance, type-safe)
- **Storage:** Redis + PostgreSQL hybrid (fast + persistent)
- **Media:** MinIO object storage (S3-compatible)
- **Frontend:** React + TypeScript (modern, responsive)
- **Monitoring:** Real-time health checks and metrics
- **API:** RESTful with comprehensive documentation

---

## 📝 Notes for Presenter

- **Practice the flow** before the actual demo
- **Have seed data ready** (run `scripts/seed-comprehensive-demo.sh`)
- **Check system health** before starting
- **Be ready to answer questions** about:
  - Scalability (how many screens/campaigns can it handle?)
  - Performance (response times, throughput)
  - Pricing model (if applicable)
  - Integration capabilities
- **Keep it conversational** - don't just read from the script
- **Highlight business value** alongside technical features

---

**Good luck with your demo! 🚀**

