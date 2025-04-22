INSERT INTO telegram_bots (username, token, enable)
SELECT 'multi_tg_test_001_bot', '7567578426:AAFoT93QBEw2lCxGItmQvF1PtafrUWXSVb4', TRUE
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM telegram_bots WHERE username = 'multi_tg_test_001_bot');

INSERT INTO telegram_bots (username, token, enable)
SELECT 'multi_tg_test_002_bot', '8154676472:AAGn_HD2pJolNd-Fjpww9B5f54tu7cocITk', TRUE
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM telegram_bots WHERE username = 'multi_tg_test_002_bot');