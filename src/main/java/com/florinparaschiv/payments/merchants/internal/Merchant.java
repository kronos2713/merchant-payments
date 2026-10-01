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

        String iban = required(settlementIban, "settlementIban")
                .replaceAll("\\s", "").toUpperCase(Locale.ROOT);
        if (!IbanValidator.isValid(iban)) {
            throw new InvalidMerchantDataException("settlementIban is not a valid IBAN");
        }

        return new Merchant(
                UUID.randomUUID(),
                name,
                required(registrationCountry, "registrationCountry").strip().toUpperCase(Locale.ROOT),
                required(registrationNumber, "registrationNumber")
                        .replaceAll("[\\s.]", "").toUpperCase(Locale.ROOT),
                iban,
                clock.instant());
    }

    private static String required(String value, String field) {
        if (value == null) {
            throw new InvalidMerchantDataException(field + " is required");
        }
        return value;
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