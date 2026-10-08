package com.medifind.dto;

/**
 * SLP: Medicine Search & Pharmacy Discovery → "Integrate mapping/
 * geolocation service"
 */
public class PharmacyMapPoint {

    private Long id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private boolean verified;
    private String openingHours;

    public PharmacyMapPoint() {
    }

    public PharmacyMapPoint(Long id, String name, String address, double latitude,
                            double longitude, boolean verified, String openingHours) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.verified = verified;
        this.openingHours = openingHours;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }
}