-- The promille calculation divides by the weight.
alter table users add constraint users_weight_positive check (weight > 0);
