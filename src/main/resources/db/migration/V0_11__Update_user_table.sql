ALTER TABLE "user" ADD COLUMN delay_penalty_id VARCHAR;

ALTER TABLE "user" ADD CONSTRAINT fk_user_delay_penalty
    FOREIGN KEY (delay_penalty_id)
        REFERENCES delay_penalty(id);

