create table payment_requests (
                                  id                 uuid         primary key,
                                  merchant_id        uuid         not null,
                                  amount_minor       bigint       not null,
                                  currency           varchar(3)   not null,
                                  description        varchar(140) not null,
                                  merchant_reference varchar(35)  not null,
                                  status             varchar(16)  not null,
                                  expires_at         timestamptz  not null,
                                  paid_at            timestamptz,
                                  version            bigint       not null,
                                  created_at         timestamptz  not null,
                                  updated_at         timestamptz  not null,

                                  constraint fk_payment_requests_merchant
                                      foreign key (merchant_id) references merchants (id),
                                  constraint ck_payment_requests_amount_positive
                                      check (amount_minor > 0),
                                  constraint ck_payment_requests_currency
                                      check (currency = 'EUR'),
                                  constraint ck_payment_requests_status
                                      check (status in ('CREATED', 'PAID', 'CANCELLED', 'EXPIRED')),
                                  constraint ck_payment_requests_paid_at
                                      check ((status = 'PAID') = (paid_at is not null)),
                                  constraint ck_payment_requests_expiry_after_creation
                                      check (expires_at > created_at),
                                  constraint uq_payment_requests_merchant_reference
                                      unique (merchant_id, merchant_reference)
);