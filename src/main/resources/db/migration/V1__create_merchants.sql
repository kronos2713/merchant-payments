create table merchants (
                           id                   uuid         primary key,
                           legal_name           varchar(256) not null,
                           registration_country varchar(2)   not null,
                           registration_number  varchar(64)  not null,
                           settlement_iban      varchar(34)  not null,
                           status               varchar(16)  not null,
                           version              bigint       not null,
                           created_at           timestamptz  not null,
                           updated_at           timestamptz  not null,

                           constraint ck_merchants_registration_country
                               check (registration_country ~ '^[A-Z]{2}$'),
    constraint ck_merchants_registration_number
        check (registration_number ~ '^[A-Z0-9]+$'),
    constraint ck_merchants_settlement_iban
        check (settlement_iban ~ '^[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}$'),
    constraint ck_merchants_status
        check (status in ('PENDING', 'ACTIVE', 'SUSPENDED')),
    constraint uq_merchants_registration
        unique (registration_country, registration_number)
);