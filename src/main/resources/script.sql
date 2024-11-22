-- ALTER TABLE reciept_tb
--     DROP CONSTRAINT fksffxewqqbpftkxxbtgmqqhrck,
--     ADD CONSTRAINT fksffxewqqbpftkxxbtgmqqhrck
--         FOREIGN KEY (purchase_id)
--             REFERENCES purchase_tb (id)
--             ON DELETE CASCADE;
--
-- ALTER TABLE notification_tb
--     DROP CONSTRAINT fkjfbibej4isktrijv7tbbsle65,
--     ADD CONSTRAINT fkjfbibej4isktrijv7tbbsle65
--         FOREIGN KEY (recipe_id)
--             REFERENCES reciept_tb (id)
--             ON DELETE CASCADE;
--
-- ALTER TABLE address_tb
--     DROP CONSTRAINT fk79r2osjq808rvy407533do5i9,
--     ADD CONSTRAINT fk79r2osjq808rvy407533do5i9
--         FOREIGN KEY (user_id)
--             REFERENCES user_tb (id)
--             ON DELETE CASCADE;
--
-- ALTER TABLE feedback_tb
--     DROP CONSTRAINT fk3exy75beew66jgdj9xiappwfq,
--     ADD CONSTRAINT fk3exy75beew66jgdj9xiappwfq
--         FOREIGN KEY (user_id)
--             REFERENCES user_tb (id)
--             ON DELETE CASCADE;
--
-- ALTER TABLE food_recipe_tb
--     DROP CONSTRAINT fkch8f6uwhy7atp6jv5dvgfruf5,
--     ADD CONSTRAINT fkch8f6uwhy7atp6jv5dvgfruf5
--         FOREIGN KEY (user_id)
--             REFERENCES user_tb (id)
--             ON DELETE CASCADE;
--

-- Alter address_tb
ALTER TABLE public.address_tb
    DROP CONSTRAINT fk79r2osjq808rvy407533do5i9,
    ADD CONSTRAINT fk79r2osjq808rvy407533do5i9
        FOREIGN KEY (user_id)
            REFERENCES public.user_tb (id)
            ON DELETE CASCADE;

-- Alter bank_tb
ALTER TABLE public.bank_tb
    DROP CONSTRAINT fk7s8jfg6babg59aytcupjwjjyb,
    ADD CONSTRAINT fk7s8jfg6babg59aytcupjwjjyb
        FOREIGN KEY (user_id)
            REFERENCES public.user_tb (id)
            ON DELETE CASCADE;

-- Alter code_tb
ALTER TABLE public.code_tb
    DROP CONSTRAINT fk3l5omi2n2hdbrvx0xtst5ryph,
    ADD CONSTRAINT fk3l5omi2n2hdbrvx0xtst5ryph
        FOREIGN KEY (user_id)
            REFERENCES public.user_tb (id)
            ON DELETE CASCADE;

-- Alter credential_tb
ALTER TABLE public.credential_tb
    DROP CONSTRAINT fkfmpjo1smq9jpbj7qh1ckp5ih4,
    ADD CONSTRAINT fkfmpjo1smq9jpbj7qh1ckp5ih4
        FOREIGN KEY (user_id)
            REFERENCES public.user_tb (id)
            ON DELETE CASCADE;

-- Alter device_token_tb
ALTER TABLE public.device_token_tb
    DROP CONSTRAINT fke2w9but28wx494h0q6373sc6b,
    ADD CONSTRAINT fke2w9but28wx494h0q6373sc6b
        FOREIGN KEY (user_id)
            REFERENCES public.user_tb (id)
            ON DELETE CASCADE;

-- Alter food_recipe_tb
ALTER TABLE public.food_recipe_tb
    DROP CONSTRAINT fkch8f6uwhy7atp6jv5dvgfruf5,
    ADD CONSTRAINT fkch8f6uwhy7atp6jv5dvgfruf5
        FOREIGN KEY (user_id)
            REFERENCES public.user_tb (id)
            ON DELETE CASCADE;

-- Alter food_sell_tb
ALTER TABLE public.food_sell_tb
    DROP CONSTRAINT fkjnh07eurlqfsgfrukru7l94h,
    ADD CONSTRAINT fkjnh07eurlqfsgfrukru7l94h
        FOREIGN KEY (food_recipe_id)
            REFERENCES public.food_recipe_tb (id)
            ON DELETE CASCADE;

-- Alter favorite_tb
ALTER TABLE public.favorite_tb
    DROP CONSTRAINT fkf8yje2vlq4aswj92vxu1jsf42,
    ADD CONSTRAINT fkf8yje2vlq4aswj92vxu1jsf42
        FOREIGN KEY (user_id)
            REFERENCES public.user_tb (id)
            ON DELETE CASCADE;

ALTER TABLE public.favorite_tb
    DROP CONSTRAINT fkomhtq2agok4elcodsj19rtud0,
    ADD CONSTRAINT fkomhtq2agok4elcodsj19rtud0
        FOREIGN KEY (food_recipe_id)
            REFERENCES public.food_recipe_tb (id)
            ON DELETE CASCADE;

ALTER TABLE public.favorite_tb
    DROP CONSTRAINT fkgffs66bsr5oxtdtvhau5pijic,
    ADD CONSTRAINT fkgffs66bsr5oxtdtvhau5pijic
        FOREIGN KEY (food_sell_id)
            REFERENCES public.food_sell_tb (id)
            ON DELETE CASCADE;

-- Alter feedback_tb
ALTER TABLE public.feedback_tb
    DROP CONSTRAINT fk3exy75beew66jgdj9xiappwfq,
    ADD CONSTRAINT fk3exy75beew66jgdj9xiappwfq
        FOREIGN KEY (user_id)
            REFERENCES public.user_tb (id)
            ON DELETE CASCADE;

ALTER TABLE public.feedback_tb
    DROP CONSTRAINT fk3og1myjvr5pcg315l0uqfalw9,
    ADD CONSTRAINT fk3og1myjvr5pcg315l0uqfalw9
        FOREIGN KEY (food_recipe_id)
            REFERENCES public.food_recipe_tb (id)
            ON DELETE CASCADE;

ALTER TABLE public.feedback_tb
    DROP CONSTRAINT fkqxgiy5exninf3usqv8x5avwet,
    ADD CONSTRAINT fkqxgiy5exninf3usqv8x5avwet
        FOREIGN KEY (food_sell_id)
            REFERENCES public.food_sell_tb (id)
            ON DELETE CASCADE;

-- Alter photo_tb
ALTER TABLE public.photo_tb
    DROP CONSTRAINT fkqfns2xv28h7si4tlxt4gljey7,
    ADD CONSTRAINT fkqfns2xv28h7si4tlxt4gljey7
        FOREIGN KEY (food_recipe_id)
            REFERENCES public.food_recipe_tb (id)
            ON DELETE CASCADE;

-- Alter purchase_tb
ALTER TABLE public.purchase_tb
    DROP CONSTRAINT fkqo8kfayecux21c1troa12pgu2,
    ADD CONSTRAINT fkqo8kfayecux21c1troa12pgu2
        FOREIGN KEY (buyer_id)
            REFERENCES public.user_tb (id)
            ON DELETE CASCADE;

ALTER TABLE public.purchase_tb
    DROP CONSTRAINT fk8j0pxybywfdorrf2l0nxphkvg,
    ADD CONSTRAINT fk8j0pxybywfdorrf2l0nxphkvg
        FOREIGN KEY (food_sell_id)
            REFERENCES public.food_sell_tb (id)
            ON DELETE CASCADE;

-- Alter receipt_tb
ALTER TABLE public.reciept_tb
    DROP CONSTRAINT fksffxewqqbpftkxxbtgmqqhrck,
    ADD CONSTRAINT fksffxewqqbpftkxxbtgmqqhrck
        FOREIGN KEY (purchase_id)
            REFERENCES public.purchase_tb (id)
            ON DELETE CASCADE;

-- Alter notification_tb
ALTER TABLE public.notification_tb
    DROP CONSTRAINT fkaplx7mt7tbbayd9fgx8t44q8k,
    ADD CONSTRAINT fkaplx7mt7tbbayd9fgx8t44q8k
        FOREIGN KEY (receiver_id)
            REFERENCES public.user_tb (id)
            ON DELETE CASCADE;

ALTER TABLE public.notification_tb
    DROP CONSTRAINT fkidsrwj1p4daglfbb2birwya5f,
    ADD CONSTRAINT fkidsrwj1p4daglfbb2birwya5f
        FOREIGN KEY (sender_id)
            REFERENCES public.user_tb (id)
            ON DELETE CASCADE;

ALTER TABLE public.notification_tb
    DROP CONSTRAINT fkjfbibej4isktrijv7tbbsle65,
    ADD CONSTRAINT fkjfbibej4isktrijv7tbbsle65
        FOREIGN KEY (recipe_id)
            REFERENCES public.reciept_tb (id)
            ON DELETE CASCADE;

-- Alter token_tb
ALTER TABLE public.token_tb
    DROP CONSTRAINT fkb4kmvil2dffrtryldinm1f9vk,
    ADD CONSTRAINT fkb4kmvil2dffrtryldinm1f9vk
        FOREIGN KEY (user_id)
            REFERENCES public.user_tb (id)
            ON DELETE CASCADE;
