package drinkcounter.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** The place and radius within which others can join a party; a party has at most one. */
@Entity
public class PartyLocationInvite {
    private Integer partyId;
    private double latitude;
    private double longitude;
    private int radiusM;
    private Integer createdBy;
    private Instant createdAt;

    @Id
    public Integer getPartyId() {
        return partyId;
    }

    public void setPartyId(Integer partyId) {
        this.partyId = partyId;
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

    @Column(name = "radius_m")
    public int getRadiusM() {
        return radiusM;
    }

    public void setRadiusM(int radiusM) {
        this.radiusM = radiusM;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
