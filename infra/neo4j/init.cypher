// ============================================
// AnchorIQ Neo4j Indexes + Constraints
// ============================================

// Vessel indexes
CREATE INDEX vessel_imo IF NOT EXISTS FOR (v:Vessel) ON (v.imo);
CREATE INDEX vessel_mmsi IF NOT EXISTS FOR (v:Vessel) ON (v.mmsi);
CREATE INDEX vessel_type IF NOT EXISTS FOR (v:Vessel) ON (v.type);
CREATE INDEX vessel_flag IF NOT EXISTS FOR (v:Vessel) ON (v.flag);

// Vessel unique constraints
CREATE CONSTRAINT vessel_imo_unique IF NOT EXISTS FOR (v:Vessel) REQUIRE v.imo IS UNIQUE;
CREATE CONSTRAINT vessel_mmsi_unique IF NOT EXISTS FOR (v:Vessel) REQUIRE v.mmsi IS UNIQUE;

// Port indexes
CREATE INDEX port_locode IF NOT EXISTS FOR (p:Port) ON (p.locode);
CREATE CONSTRAINT port_locode_unique IF NOT EXISTS FOR (p:Port) REQUIRE p.locode IS UNIQUE;

// Company indexes
CREATE INDEX company_name IF NOT EXISTS FOR (c:Company) ON (c.name);

// Country indexes
CREATE INDEX country_code IF NOT EXISTS FOR (c:Country) ON (c.code);
CREATE CONSTRAINT country_code_unique IF NOT EXISTS FOR (c:Country) REQUIRE c.code IS UNIQUE;

// Sanction indexes
CREATE INDEX sanction_ref IF NOT EXISTS FOR (s:Sanction) ON (s.referenceNumber);

// SeaZone spatial index
CREATE POINT INDEX seazone_location IF NOT EXISTS FOR (sz:SeaZone) ON (sz.location);

// ============================================
// Chokepoints (static data)
// ============================================

CREATE (c:Chokepoint {
  name: 'Hormuz',
  displayName: 'Strait of Hormuz',
  lat: 26.5667,
  lon: 56.2500,
  riskLevel: 'HIGH',
  description: 'Between Iran and Oman, critical for global crude oil transport'
});

CREATE (c:Chokepoint {
  name: 'Malacca',
  displayName: 'Strait of Malacca',
  lat: 2.5000,
  lon: 101.2000,
  riskLevel: 'MEDIUM',
  description: 'Between Malaysia and Indonesia, shortest Asia-Europe route'
});

CREATE (c:Chokepoint {
  name: 'Suez',
  displayName: 'Suez Canal',
  lat: 30.4500,
  lon: 32.3500,
  riskLevel: 'LOW',
  description: 'Egypt, critical Asia-Europe shipping lane'
});

CREATE (c:Chokepoint {
  name: 'Bab el-Mandeb',
  displayName: 'Bab el-Mandeb Strait',
  lat: 12.5833,
  lon: 43.3333,
  riskLevel: 'HIGH',
  description: 'Between Yemen and Djibouti, gateway to Suez Canal'
});

CREATE (c:Chokepoint {
  name: 'Taiwan Strait',
  displayName: 'Taiwan Strait',
  lat: 24.0000,
  lon: 119.0000,
  riskLevel: 'MEDIUM',
  description: 'Between China and Taiwan, critical for semiconductor supply chain'
});

CREATE (c:Chokepoint {
  name: 'Panama',
  displayName: 'Panama Canal',
  lat: 9.1000,
  lon: -79.6800,
  riskLevel: 'LOW',
  description: 'Pacific-Atlantic connection, critical for US East Coast routes'
});

// ============================================
// Major Routes (Korea-based key 3)
// ============================================

CREATE (r:Route {
  name: 'Asia-MidEast',
  displayName: 'Asia-Middle East (Crude Oil)',
  distance: 6500,
  unit: 'nm'
});

CREATE (r:Route {
  name: 'Asia-Europe',
  displayName: 'Asia-Europe (Export)',
  distance: 11000,
  unit: 'nm'
});

CREATE (r:Route {
  name: 'Asia-Americas',
  displayName: 'Asia-Americas (Export)',
  distance: 5500,
  unit: 'nm'
});

// Route-Chokepoint relationships
MATCH (r:Route {name: 'Asia-MidEast'}), (cp:Chokepoint {name: 'Malacca'})
CREATE (r)-[:PASSES_THROUGH {order: 1}]->(cp);

MATCH (r:Route {name: 'Asia-MidEast'}), (cp:Chokepoint {name: 'Hormuz'})
CREATE (r)-[:PASSES_THROUGH {order: 2}]->(cp);

MATCH (r:Route {name: 'Asia-Europe'}), (cp:Chokepoint {name: 'Malacca'})
CREATE (r)-[:PASSES_THROUGH {order: 1}]->(cp);

MATCH (r:Route {name: 'Asia-Europe'}), (cp:Chokepoint {name: 'Bab el-Mandeb'})
CREATE (r)-[:PASSES_THROUGH {order: 2}]->(cp);

MATCH (r:Route {name: 'Asia-Europe'}), (cp:Chokepoint {name: 'Suez'})
CREATE (r)-[:PASSES_THROUGH {order: 3}]->(cp);

MATCH (r:Route {name: 'Asia-Americas'}), (cp:Chokepoint {name: 'Panama'})
CREATE (r)-[:PASSES_THROUGH {order: 1}]->(cp);
