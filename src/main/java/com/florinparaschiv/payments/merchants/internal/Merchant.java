package com.florinparaschiv.payments.merchants.internal;

import com.florinparaschiv.payments.merchants.MerchantStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    private UUID id;

    private String legalName;
    private String registrationCountry;
    private String registrationNumber;
    private String settlementIban;

    @Enumerated(EnumType.STRING)
    private MerchantStatus status;

    @Version
    private Long version;

    private Instant createdAt;
    private Instant updatedAt;

    protected Merchant() {}

    private Merchant(UUID id, String legalName, String registrationCountry,
                     String registrationNumber, String settlementIban, Instant now) {
        this.id = id;
        this.legalName = legalName;
        this.registrationCountry = registrationCountry;
        this.registrationNumber = registrationNumber;
        this.settlementIban = settlementIban;
        this.status = MerchantStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Merchant register(String legalName, String registrationCountry,
                                    String registrationNumber, String settlementIban,
                                    Clock clock) {
        String name = required(legalName, "legalName").strip();
        if (name.isEmpty() || name.length() > 256) {
            throw new InvalidMerchantDataException("legalName must be 1 to 256 characters");
        }

        String iban = IbanValidator.normalise(required(settlementIban, "settlementIban"));
        if (!IbanValidator.isValid(iban)) {
            throw new InvalidMerchantDataException("settlementIban is not a valid IBAN");
        }

        String country = required(registrationCountry, "registrationCountry")
                .strip().toUpperCase(Locale.ROOT);
        if (!RegistrationRules.isSupportedCountry(country)) {
            throw new InvalidMerchantDataException(
                    "registrationCountry " + country + " is not supported yet");
        }

        String number = required(registrationNumber, "registrationNumber")
                .replaceAll("[\\s.]", "").toUpperCase(Locale.ROOT);
        if (!RegistrationRules.isValidNumber(country, number)) {
            throw new InvalidMerchantDataException(
                    "registrationNumber has an invalid format for " + country);
        }

        return new Merchant(UUID.randomUUID(), name, country, number, iban, clock.instant());
    }

    private static String required(String value, String field) {
        if (value == null) {
            throw new InvalidMerchantDataException(field + " is required");
        }
        return value;
    }

    public void activate(Clock clock) {
        if (status == MerchantStatus.ACTIVE) {
            return;
        }
        if (status != MerchantStatus.PENDING) {
            throw new IllegalMerchantTransitionException("activate", status);
        }
        changeStatus(MerchantStatus.ACTIVE, clock);
    }

    public void suspend(Clock clock) {
        if (status == MerchantStatus.SUSPENDED) {
            return;
        }
        if (status != MerchantStatus.ACTIVE) {
            throw new IllegalMerchantTransitionException("suspend", status);
        }
        changeStatus(MerchantStatus.SUSPENDED, clock);
    }

    public void reactivate(Clock clock) {
        if (status == MerchantStatus.ACTIVE) {
            return;
        }
        if (status != MerchantStatus.SUSPENDED) {
            throw new IllegalMerchantTransitionException("reactivate", status);
        }
        changeStatus(MerchantStatus.ACTIVE, clock);
    }

    private void changeStatus(MerchantStatus newStatus, Clock clock) {
        this.status = newStatus;
        this.updatedAt = clock.instant();
    }

    public UUID getId() { return id; }
    public String getLegalName() { return legalName; }
    public String getRegistrationCountry() { return registrationCountry; }
    public String getRegistrationNumber() { return registrationNumber; }
    public String getSettlementIban() { return settlementIban; }
    public MerchantStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Merchant other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}