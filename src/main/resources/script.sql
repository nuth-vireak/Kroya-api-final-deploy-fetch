ALTER TABLE reciept_tb
    DROP CONSTRAINT fksffxewqqbpftkxxbtgmqqhrck,
    ADD CONSTRAINT fksffxewqqbpftkxxbtgmqqhrck
        FOREIGN KEY (purchase_id)
            REFERENCES purchase_tb (id)
            ON DELETE CASCADE;

ALTER TABLE notification_tb
    DROP CONSTRAINT fkjfbibej4isktrijv7tbbsle65,
    ADD CONSTRAINT fkjfbibej4isktrijv7tbbsle65
        FOREIGN KEY (recipe_id)
            REFERENCES reciept_tb (id)
            ON DELETE CASCADE;

ALTER TABLE address_tb
    DROP CONSTRAINT fk79r2osjq808rvy407533do5i9,
    ADD CONSTRAINT fk79r2osjq808rvy407533do5i9
        FOREIGN KEY (user_id)
            REFERENCES user_tb (id)
            ON DELETE CASCADE;

ALTER TABLE feedback_tb
    DROP CONSTRAINT fk3exy75beew66jgdj9xiappwfq,
    ADD CONSTRAINT fk3exy75beew66jgdj9xiappwfq
        FOREIGN KEY (user_id)
            REFERENCES user_tb (id)
            ON DELETE CASCADE;

ALTER TABLE food_recipe_tb
    DROP CONSTRAINT fkch8f6uwhy7atp6jv5dvgfruf5,
    ADD CONSTRAINT fkch8f6uwhy7atp6jv5dvgfruf5
        FOREIGN KEY (user_id)
            REFERENCES user_tb (id)
            ON DELETE CASCADE;