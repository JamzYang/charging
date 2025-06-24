package com.ys.charging.station.domain.service;

import java.math.BigDecimal;

/**
 * 地理搜索条件
 * 
 * @author yang
 * @since 2025-06-23
 */
public class GeoSearchCriteria {
    
    private final double longitude;
    private final double latitude;
    private final double radiusKm;
    private final int maxResults;
    private final String connectorType;
    private final BigDecimal minPower;
    private final BigDecimal maxPower;
    private final String operator;
    private final boolean availableOnly;
    
    private GeoSearchCriteria(Builder builder) {
        this.longitude = builder.longitude;
        this.latitude = builder.latitude;
        this.radiusKm = builder.radiusKm;
        this.maxResults = builder.maxResults;
        this.connectorType = builder.connectorType;
        this.minPower = builder.minPower;
        this.maxPower = builder.maxPower;
        this.operator = builder.operator;
        this.availableOnly = builder.availableOnly;
    }
    
    public static GeoSearchCriteria withLimit(double longitude, double latitude, double radiusKm, int maxResults) {
        return new Builder(longitude, latitude, radiusKm, maxResults).build();
    }
    
    public static GeoSearchCriteria fastCharging(double longitude, double latitude, double radiusKm) {
        return new Builder(longitude, latitude, radiusKm, 20)
            .powerRange(BigDecimal.valueOf(50), null)
            .build();
    }
    
    public static GeoSearchCriteria superCharging(double longitude, double latitude, double radiusKm) {
        return new Builder(longitude, latitude, radiusKm, 20)
            .powerRange(BigDecimal.valueOf(150), null)
            .build();
    }
    
    public static Builder builder(double longitude, double latitude, double radiusKm) {
        return new Builder(longitude, latitude, radiusKm, 20);
    }
    
    // Getters
    public double longitude() { return longitude; }
    public double latitude() { return latitude; }
    public double radiusKm() { return radiusKm; }
    public int maxResults() { return maxResults; }
    public String getConnectorType() { return connectorType; }
    public BigDecimal getMinPower() { return minPower; }
    public BigDecimal getMaxPower() { return maxPower; }
    public String getOperator() { return operator; }
    public boolean isAvailableOnly() { return availableOnly; }
    
    public String getDescription() {
        return String.format("在 (%.6f, %.6f) 半径 %.1f 公里内搜索，最多 %d 个结果", 
            longitude, latitude, radiusKm, maxResults);
    }
    
    public static class Builder {
        private final double longitude;
        private final double latitude;
        private final double radiusKm;
        private final int maxResults;
        private String connectorType;
        private BigDecimal minPower;
        private BigDecimal maxPower;
        private String operator;
        private boolean availableOnly = false;
        
        public Builder(double longitude, double latitude, double radiusKm, int maxResults) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.radiusKm = radiusKm;
            this.maxResults = maxResults;
        }
        
        public Builder connectorType(String connectorType) {
            this.connectorType = connectorType;
            return this;
        }
        
        public Builder powerRange(BigDecimal minPower, BigDecimal maxPower) {
            this.minPower = minPower;
            this.maxPower = maxPower;
            return this;
        }
        
        public Builder operator(String operator) {
            this.operator = operator;
            return this;
        }
        
        public Builder availableOnly(boolean availableOnly) {
            this.availableOnly = availableOnly;
            return this;
        }
        
        public GeoSearchCriteria build() {
            return new GeoSearchCriteria(this);
        }
    }
}
