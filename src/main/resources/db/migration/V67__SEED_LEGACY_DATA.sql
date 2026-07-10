-- Reordered data migration: parent tables first, dependent tables later.
-- Insert order: users → groups → team → player → tournament → tournament_round →
-- tournament_player → group_member → group_tournament → group_tournament_market_type →
-- tournament_wallet → event → market → outcome → transaction

INSERT INTO resenha.users VALUES (1, 'Francisco', 'eea3143984d1130b5af1a30a9d4c4171e0015bf6b9f0f5eb1521d2da5c7aeee2', 'f2757d8d115936994f157dbe7a29de05', 'ADMIN', false, '2026-06-27 13:25:58.125026');


--
-- TOC entry 5199 (class 0 OID 41264)
-- Dependencies: 251
-- Data for Name: groups; Type: TABLE DATA; Schema: resenha; Owner: postgres
--

INSERT INTO resenha.groups VALUES (1, 'Campeonatos RIFA', true, '2026-06-27 13:25:58.125026', null, '676767');

--
-- TOC entry 5186 (class 0 OID 41082)
-- Dependencies: 238
-- Data for Name: team; Type: TABLE DATA; Schema: resenha; Owner: postgres
--

INSERT INTO resenha.team VALUES (1, 'Curacao', 'CUR', 'https://apiv3.apifootball.com/badges/520_curacao.jpg', '520', '1159');
INSERT INTO resenha.team VALUES (2, 'Cape Verde', 'CAP', 'https://apiv3.apifootball.com/badges/732_cabo-verde.jpg', '732', '1574');
INSERT INTO resenha.team VALUES (3, 'D.R. Congo', 'D.R', 'https://apiv3.apifootball.com/badges/728_congo-dr.jpg', '728', '1527');
INSERT INTO resenha.team VALUES (4, 'South Korea', 'SOU', 'https://apiv3.apifootball.com/badges/651_korea-republic.jpg', '651', '1135');
INSERT INTO resenha.team VALUES (5, 'Czech Republic', 'CZE', 'https://apiv3.apifootball.com/badges/13_czechia.jpg', '13', '651');
INSERT INTO resenha.team VALUES (6, 'Mexico', 'MEX', 'https://apiv3.apifootball.com/badges/511_mexico.jpg', '511', '594');
INSERT INTO resenha.team VALUES (7, 'South Africa', 'SOU', 'https://apiv3.apifootball.com/badges/726_south-africa.jpg', '726', '1534');
INSERT INTO resenha.team VALUES (8, 'USA', 'USA', 'https://apiv3.apifootball.com/badges/523_united-states.jpg', '523', '602');
INSERT INTO resenha.team VALUES (9, 'Paraguay', 'PAR', 'https://apiv3.apifootball.com/badges/537_paraguay.jpg', '537', '568');
INSERT INTO resenha.team VALUES (10, 'Canada', 'CAN', 'https://apiv3.apifootball.com/badges/512_canada.jpg', '512', '593');
INSERT INTO resenha.team VALUES (11, 'Bosnia & Herzegovina', 'BOS', 'https://apiv3.apifootball.com/badges/685_bosnia-herzegovina.jpg', '685', '626');
INSERT INTO resenha.team VALUES (12, 'Haiti', 'HAI', 'https://apiv3.apifootball.com/badges/515_haiti.jpg', '515', '619');
INSERT INTO resenha.team VALUES (13, 'Scotland', 'SCO', 'https://apiv3.apifootball.com/badges/15_scotland.jpg', '15', '565');
INSERT INTO resenha.team VALUES (14, 'Qatar', 'QAT', 'https://apiv3.apifootball.com/badges/538_qatar.jpg', '538', '1126');
INSERT INTO resenha.team VALUES (15, 'Switzerland', 'SWI', 'https://apiv3.apifootball.com/badges/2_switzerland.jpg', '2', '607');
INSERT INTO resenha.team VALUES (16, 'Brazil', 'BRA', 'https://apiv3.apifootball.com/badges/531_brazil.jpg', '531', '572');
INSERT INTO resenha.team VALUES (17, 'Morocco', 'MOR', 'https://apiv3.apifootball.com/badges/717_morocco.jpg', '717', '1535');
INSERT INTO resenha.team VALUES (18, 'Germany', 'GER', 'https://apiv3.apifootball.com/badges/21_germany.jpg', '21', '561');
INSERT INTO resenha.team VALUES (19, 'Sweden', 'SWE', 'https://apiv3.apifootball.com/badges/17_sweden.jpg', '17', '613');
INSERT INTO resenha.team VALUES (20, 'Tunisia', 'TUN', 'https://apiv3.apifootball.com/badges/719_tunisia.jpg', '719', '1555');
INSERT INTO resenha.team VALUES (21, 'Australia', 'AUS', 'https://apiv3.apifootball.com/badges/529_australia.jpg', '529', '1136');
INSERT INTO resenha.team VALUES (22, 'Turkey', 'TUR', 'https://apiv3.apifootball.com/badges/1_turkiye.jpg', '1', '562');
INSERT INTO resenha.team VALUES (23, 'Ivory Coast', 'IVO', 'https://apiv3.apifootball.com/badges/738_ivory-coast.jpg', '738', '1144');
INSERT INTO resenha.team VALUES (24, 'Ecuador', 'ECU', 'https://apiv3.apifootball.com/badges/541_ecuador.jpg', '541', '570');
INSERT INTO resenha.team VALUES (25, 'Netherlands', 'NET', 'https://apiv3.apifootball.com/badges/10_netherlands.jpg', '10', '556');
INSERT INTO resenha.team VALUES (26, 'Japan', 'JAP', 'https://apiv3.apifootball.com/badges/540_japan.jpg', '540', '1137');
INSERT INTO resenha.team VALUES (27, 'Spain', 'SPA', 'https://apiv3.apifootball.com/badges/19_spain.jpg', '19', '557');
INSERT INTO resenha.team VALUES (28, 'Saudi Arabia', 'SAU', 'https://apiv3.apifootball.com/badges/647_saudi-arabia.jpg', '647', '1141');
INSERT INTO resenha.team VALUES (29, 'Uruguay', 'URU', 'https://apiv3.apifootball.com/badges/539_uruguay.jpg', '539', '574');
INSERT INTO resenha.team VALUES (30, 'Belgium', 'BEL', 'https://apiv3.apifootball.com/badges/6_belgium.jpg', '6', '559');
INSERT INTO resenha.team VALUES (31, 'Egypt', 'EGY', 'https://apiv3.apifootball.com/badges/716_egypt.jpg', '716', '1524');
INSERT INTO resenha.team VALUES (32, 'Iran', 'IRA', 'https://apiv3.apifootball.com/badges/644_ir-iran.jpg', '644', '1127');
INSERT INTO resenha.team VALUES (33, 'New Zealand', 'NEW', 'https://apiv3.apifootball.com/badges/527_new-zealand.jpg', '527', '1143');
INSERT INTO resenha.team VALUES (34, 'Argentina', 'ARG', 'https://apiv3.apifootball.com/badges/536_argentina.jpg', '536', '575');
INSERT INTO resenha.team VALUES (35, 'Algeria', 'ALG', 'https://apiv3.apifootball.com/badges/734_algeria.jpg', '734', '1157');
INSERT INTO resenha.team VALUES (36, 'France', 'FRA', 'https://apiv3.apifootball.com/badges/22_france.jpg', '22', '555');
INSERT INTO resenha.team VALUES (37, 'Senegal', 'SEN', 'https://apiv3.apifootball.com/badges/720_senegal.jpg', '720', '1145');
INSERT INTO resenha.team VALUES (38, 'Iraq', 'IRA', 'https://apiv3.apifootball.com/badges/642_iraq.jpg', '642', '1134');
INSERT INTO resenha.team VALUES (39, 'Norway', 'NOR', 'https://apiv3.apifootball.com/badges/692_norway.jpg', '692', '647');
INSERT INTO resenha.team VALUES (40, 'Austria', 'AUS', 'https://apiv3.apifootball.com/badges/9_austria.jpg', '9', '566');
INSERT INTO resenha.team VALUES (41, 'Jordan', 'JOR', 'https://apiv3.apifootball.com/badges/641_jordan.jpg', '641', '1131');
INSERT INTO resenha.team VALUES (42, 'England', 'ENG', 'https://apiv3.apifootball.com/badges/16_england.jpg', '16', '632');
INSERT INTO resenha.team VALUES (43, 'Croatia', 'CRO', 'https://apiv3.apifootball.com/badges/14_croatia.jpg', '14', '554');
INSERT INTO resenha.team VALUES (44, 'Ghana', 'GHA', 'https://apiv3.apifootball.com/badges/725_ghana.jpg', '725', '1566');
INSERT INTO resenha.team VALUES (45, 'Panama', 'PAN', 'https://apiv3.apifootball.com/badges/524_panama.jpg', '524', '603');
INSERT INTO resenha.team VALUES (46, 'Uzbekistan', 'UZB', 'https://apiv3.apifootball.com/badges/646_uzbekistan.jpg', '646', '1129');
INSERT INTO resenha.team VALUES (47, 'Colombia', 'COL', 'https://apiv3.apifootball.com/badges/535_colombia.jpg', '535', '573');
INSERT INTO resenha.team VALUES (48, 'Portugal', 'POR', 'https://apiv3.apifootball.com/badges/23_portugal.jpg', '23', '553');
INSERT INTO resenha.team VALUES (49, 'Flamengo', 'FLA', 'https://apiv3.apifootball.com/badges/542_flamengo.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (50, 'Palmeiras', 'PAL', 'https://apiv3.apifootball.com/badges/546_palmeiras.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (51, 'Botafogo', 'BOT', 'https://apiv3.apifootball.com/badges/1875_botafogo.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (52, 'Corinthians', 'COR', 'https://apiv3.apifootball.com/badges/2017_corinthians.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (53, 'Arsenal', 'ARS', 'https://apiv3.apifootball.com/badges/141_arsenal-fc.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (54, 'Borussia Dortmund', 'BVB', 'https://apiv3.apifootball.com/badges/92_borussia-dortmund.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (55, 'FC Bayern München', 'BAY', 'https://apiv3.apifootball.com/badges/72_bayern-munchen.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (56, 'Inter de Milão', 'INT', 'https://apiv3.apifootball.com/badges/79_internazionale.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (57, 'Atlético de Madrid', 'ATL', 'https://apiv3.apifootball.com/badges/73_atletico-de-madrid.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (58, 'Chelsea', 'CHE', 'https://apiv3.apifootball.com/badges/88_chelsea.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (59, 'FC Barcelona', 'BAR', 'https://apiv3.apifootball.com/badges/97_barcelona.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (60, 'Atlético Mineiro', 'CAM', 'https://apiv3.apifootball.com/badges/1865_atletico-mineiro.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (61, 'Fluminense', 'FLU', 'https://apiv3.apifootball.com/badges/1879_fluminense.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (62, 'Vasco da Gama', 'VAS', 'https://apiv3.apifootball.com/badges/1877_vasco-da-gama.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (63, 'São Paulo', 'SAO', 'https://apiv3.apifootball.com/badges/556_sao-paulo.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (64, 'Liverpool', 'LIV', 'https://apiv3.apifootball.com/badges/84_liverpool.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (65, 'Real Madrid', 'REA', 'https://apiv3.apifootball.com/badges/76_real-madrid.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (66, 'Manchester City', 'MCI', 'https://apiv3.apifootball.com/badges/80_manchester-city.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (67, 'Paris Saint-Germain', 'PSG', 'https://apiv3.apifootball.com/badges/100_psg.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (68, 'Vitória', 'VIT', 'https://apiv3.apifootball.com/badges/1748_vitoria.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (69, 'Confiança', 'CON', 'https://apiv3.apifootball.com/badges/1749_confianca.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (70, 'Fortaleza', 'FOR', 'https://apiv3.apifootball.com/badges/2018_fortaleza.jpg', NULL, NULL);
INSERT INTO resenha.team VALUES (71, 'Sampaio Corrêa', 'SAM', 'https://logodetimes.com/times/sampaio-correa/logo-sampaio-correa-4096.png', NULL, NULL);
INSERT INTO resenha.team VALUES (72, 'Ceará', 'CEA', 'https://logodetimes.com/times/ceara/logo-ceara-4096.png', NULL, NULL);
INSERT INTO resenha.team VALUES (73, 'CSA', 'CSA', 'https://logodetimes.com/times/csa/logo-csa-4096.png', NULL, NULL);
INSERT INTO resenha.team VALUES (76, 'Botafogo - PB', 'BPB', 'https://logodetimes.com/times/botafogo-pb/logo-botafogo-pb-4096.png', NULL, NULL);
INSERT INTO resenha.team VALUES (74, 'CRB', 'CRB', 'https://logodetimes.com/times/crb/logo-crb-4096.png', NULL, NULL);
INSERT INTO resenha.team VALUES (75, 'Sport Recife', 'SPO', 'https://logodetimes.com/times/sport-recife/logo-sport-recife-4096.png', NULL, NULL);
INSERT INTO resenha.team VALUES (77, 'Maranhão', 'MAC', 'https://upload.wikimedia.org/wikipedia/pt/6/6d/Maranh%C3%A3o_Atl%C3%A9tico_Clube.png', NULL, NULL);
INSERT INTO resenha.team VALUES (78, 'Náutico', 'NAU', 'https://upload.wikimedia.org/wikipedia/pt/d/de/Simbolo-escudo-nautico.png', NULL, NULL);
INSERT INTO resenha.team VALUES (79, 'Bahia', 'BAH', 'https://media-bahia.s3.amazonaws.com/wp-content/uploads/2024/09/26125306/Escudo-Bahia-848x1024.webp', NULL, NULL);

--
-- TOC entry 5173 (class 0 OID 40951)
-- Dependencies: 225
-- Data for Name: player; Type: TABLE DATA; Schema: resenha; Owner: postgres
--

INSERT INTO resenha.player VALUES (1, 'Alexandre', true, 981.64, NULL, 1);
INSERT INTO resenha.player VALUES (2, 'Bichas', true, 981.43, NULL, 1);
INSERT INTO resenha.player VALUES (3, 'Bruno', true, 984.00, NULL, 1);
INSERT INTO resenha.player VALUES (4, 'Conrado', true, 1012.80, NULL, 1);
INSERT INTO resenha.player VALUES (5, 'Fitaroni', true, 1119.01, NULL, 1);
INSERT INTO resenha.player VALUES (6, 'Francisco', true, 1062.95, NULL, 1);
INSERT INTO resenha.player VALUES (7, 'Gustavo', true, 984.00, NULL, 1);
INSERT INTO resenha.player VALUES (8, 'Kadu', true, 980.00, NULL, 1);
INSERT INTO resenha.player VALUES (9, 'Leo', true, 950.20, NULL, 1);
INSERT INTO resenha.player VALUES (10, 'Luan', true, 985.98, NULL, 1);
INSERT INTO resenha.player VALUES (11, 'Lucas', true, 1044.99, NULL, 1);
INSERT INTO resenha.player VALUES (12, 'Macario', true, 1008.98, NULL, 1);
INSERT INTO resenha.player VALUES (13, 'Nicolas', true, 1011.44, NULL, 1);
INSERT INTO resenha.player VALUES (14, 'Paulo', true, 917.37, NULL, 1);
INSERT INTO resenha.player VALUES (15, 'Puskas', true, 1008.85, NULL, 1);
INSERT INTO resenha.player VALUES (16, 'Ribas', true, 968.40, NULL, 1);
INSERT INTO resenha.player VALUES (17, 'Tadeu', true, 1013.96, NULL, 1);
INSERT INTO resenha.player VALUES (18, 'Vini', true, 984.00, NULL, 1);

--
-- TOC entry 5175 (class 0 OID 40967)
-- Dependencies: 227
-- Data for Name: tournament; Type: TABLE DATA; Schema: resenha; Owner: postgres
--

INSERT INTO resenha.tournament VALUES (1, 'd389720a-82b9-5c7e-b0a8-db8435a32cce', 'Copa XerecaBet do Bostil 2024', 'FIFA_MATCH', 'COMPLETED', NULL, NULL, '2025-07-14 21:42:45.693', 'BRACKET', 'MANUAL', true, 1, 2, 0, NULL);
INSERT INTO resenha.tournament VALUES (2, 'f6f07078-dc47-5974-bd5c-c0557c11c2c1', 'Resenha League 2025', 'FIFA_MATCH', 'COMPLETED', NULL, NULL, '2025-07-14 21:51:26.857', 'LEAGUE_BRACKET', 'MANUAL', false, 1, 2, 0, NULL);
INSERT INTO resenha.tournament VALUES (3, '701f530c-64d2-59d6-9fdf-825541d6f1a5', 'Mundial de Púbis 2025', 'FIFA_MATCH', 'COMPLETED', NULL, NULL, '2025-07-20 00:54:30.175', 'BRACKET', 'MANUAL', false, 1, 2, 0, NULL);
INSERT INTO resenha.tournament VALUES (4, '54371e7f-e27a-557d-9358-8230c66bf7fd', 'Copa Intercontinental da RIFA Lokal™ 2025', 'FIFA_MATCH', 'COMPLETED', NULL, NULL, '2025-07-20 01:03:42.276', 'BRACKET', 'MANUAL', false, 1, 2, 0, NULL);
INSERT INTO resenha.tournament VALUES (5, 'c8add6ac-1280-4b67-bc3c-5582351015e2', 'Copa Mateus do Nordeste 2025', 'FIFA_MATCH', 'COMPLETED', '2026-06-27 14:06:17.325662', '2026-06-27 14:21:36.331086', NULL, 'BRACKET', 'MANUAL', false, 1, 2, 2, NULL);

--
-- TOC entry 5177 (class 0 OID 40978)
-- Dependencies: 229
-- Data for Name: tournament_round; Type: TABLE DATA; Schema: resenha; Owner: postgres
--

INSERT INTO resenha.tournament_round VALUES (1, 1, 'Quartas de Final', 1.0000, 0, 'KNOCKOUT', NULL);
INSERT INTO resenha.tournament_round VALUES (2, 1, 'Semifinal', 1.2000, 1, 'KNOCKOUT', NULL);
INSERT INTO resenha.tournament_round VALUES (3, 1, 'Final', 1.4000, 2, 'KNOCKOUT', NULL);
INSERT INTO resenha.tournament_round VALUES (4, 1, 'Disputa de Terceiro Lugar', 1.2000, 3, 'KNOCKOUT', NULL);
INSERT INTO resenha.tournament_round VALUES (5, 2, 'Liga', 1.0000, 0, 'GROUP_STAGE', NULL);
INSERT INTO resenha.tournament_round VALUES (6, 2, 'Quartas de Final', 1.0000, 1, 'KNOCKOUT', NULL);
INSERT INTO resenha.tournament_round VALUES (7, 2, 'Semifinal', 1.2000, 2, 'KNOCKOUT', NULL);
INSERT INTO resenha.tournament_round VALUES (8, 2, 'Final', 1.4000, 3, 'KNOCKOUT', NULL);
INSERT INTO resenha.tournament_round VALUES (9, 3, 'Play-in', 1.0000, 0, 'KNOCKOUT', NULL);
INSERT INTO resenha.tournament_round VALUES (10, 3, 'Quartas de Final', 1.0000, 1, 'KNOCKOUT', NULL);
INSERT INTO resenha.tournament_round VALUES (11, 3, 'Semifinal', 1.2000, 2, 'KNOCKOUT', NULL);
INSERT INTO resenha.tournament_round VALUES (12, 3, 'Final', 1.4000, 3, 'KNOCKOUT', NULL);
INSERT INTO resenha.tournament_round VALUES (13, 4, 'Play-in', 1.0000, 0, 'KNOCKOUT', NULL);
INSERT INTO resenha.tournament_round VALUES (14, 4, 'Semifinal', 1.0000, 1, 'KNOCKOUT', NULL);
INSERT INTO resenha.tournament_round VALUES (15, 4, 'Final', 1.0000, 2, 'KNOCKOUT', NULL);
INSERT INTO resenha.tournament_round VALUES (17, 5, 'Quartas de Final', 1.2000, 2, 'KNOCKOUT', NULL);
INSERT INTO resenha.tournament_round VALUES (18, 5, 'Semifinais', 1.4000, 3, 'KNOCKOUT', NULL);
INSERT INTO resenha.tournament_round VALUES (19, 5, 'Final', 2.0000, 4, 'KNOCKOUT', NULL);
INSERT INTO resenha.tournament_round VALUES (16, 5, 'Play-in', 1.0000, 1, 'KNOCKOUT', NULL);

--
-- TOC entry 5178 (class 0 OID 40989)
-- Dependencies: 230
-- Data for Name: tournament_player; Type: TABLE DATA; Schema: resenha; Owner: postgres
--

INSERT INTO resenha.tournament_player VALUES (1, 1, 1, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (1, 2, 2, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (1, 5, 5, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (1, 9, 9, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (1, 10, 10, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (1, 11, 11, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (1, 12, 12, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (1, 14, 14, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (1, 15, 15, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (1, 16, 16, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (3, 1, 37, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (3, 3, 39, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (3, 4, 40, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (3, 7, 43, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (3, 15, 51, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (3, 16, 52, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (3, 18, 54, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (4, 1, 55, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (4, 3, 57, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (4, 4, 58, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (4, 5, 59, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (4, 6, 60, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (4, 7, 61, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (4, 11, 65, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (4, 12, 66, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (4, 13, 67, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (4, 15, 69, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (4, 16, 70, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (4, 18, 72, NULL, NULL);
INSERT INTO resenha.tournament_player VALUES (2, 3, 21, NULL, 1);
INSERT INTO resenha.tournament_player VALUES (2, 4, 22, NULL, 1);
INSERT INTO resenha.tournament_player VALUES (2, 7, 25, NULL, 1);
INSERT INTO resenha.tournament_player VALUES (2, 10, 28, NULL, 1);
INSERT INTO resenha.tournament_player VALUES (2, 11, 29, NULL, 1);
INSERT INTO resenha.tournament_player VALUES (2, 13, 31, NULL, 1);
INSERT INTO resenha.tournament_player VALUES (2, 18, 36, NULL, 1);
INSERT INTO resenha.tournament_player VALUES (3, 2, 38, 57, NULL);
INSERT INTO resenha.tournament_player VALUES (3, 11, 47, 53, NULL);
INSERT INTO resenha.tournament_player VALUES (3, 14, 50, 49, NULL);
INSERT INTO resenha.tournament_player VALUES (3, 10, 46, 58, NULL);
INSERT INTO resenha.tournament_player VALUES (3, 17, 53, 59, NULL);
INSERT INTO resenha.tournament_player VALUES (3, 5, 41, 54, NULL);
INSERT INTO resenha.tournament_player VALUES (3, 6, 42, 50, NULL);
INSERT INTO resenha.tournament_player VALUES (3, 12, 48, 51, NULL);
INSERT INTO resenha.tournament_player VALUES (3, 8, 44, 55, NULL);
INSERT INTO resenha.tournament_player VALUES (3, 13, 49, 52, NULL);
INSERT INTO resenha.tournament_player VALUES (3, 9, 45, 56, NULL);
INSERT INTO resenha.tournament_player VALUES (2, 15, 33, 54, 1);
INSERT INTO resenha.tournament_player VALUES (2, 5, 23, 59, 1);
INSERT INTO resenha.tournament_player VALUES (2, 6, 24, 56, 1);
INSERT INTO resenha.tournament_player VALUES (1, 6, 6, 50, NULL);
INSERT INTO resenha.tournament_player VALUES (1, 4, 4, 51, NULL);
INSERT INTO resenha.tournament_player VALUES (1, 13, 13, 52, NULL);
INSERT INTO resenha.tournament_player VALUES (1, 17, 17, 61, NULL);
INSERT INTO resenha.tournament_player VALUES (1, 8, 8, 49, NULL);
INSERT INTO resenha.tournament_player VALUES (1, 7, 7, 62, NULL);
INSERT INTO resenha.tournament_player VALUES (1, 18, 18, 63, NULL);
INSERT INTO resenha.tournament_player VALUES (1, 3, 3, 60, NULL);
INSERT INTO resenha.tournament_player VALUES (4, 8, 62, 55, NULL);
INSERT INTO resenha.tournament_player VALUES (4, 14, 68, 49, NULL);
INSERT INTO resenha.tournament_player VALUES (4, 9, 63, 56, NULL);
INSERT INTO resenha.tournament_player VALUES (4, 17, 71, 59, NULL);
INSERT INTO resenha.tournament_player VALUES (4, 10, 64, 58, NULL);
INSERT INTO resenha.tournament_player VALUES (4, 2, 56, 57, NULL);
INSERT INTO resenha.tournament_player VALUES (2, 1, 19, 55, 1);
INSERT INTO resenha.tournament_player VALUES (2, 8, 26, 53, 1);
INSERT INTO resenha.tournament_player VALUES (2, 14, 32, 58, 1);
INSERT INTO resenha.tournament_player VALUES (2, 17, 35, 67, 1);
INSERT INTO resenha.tournament_player VALUES (2, 2, 20, 66, 1);
INSERT INTO resenha.tournament_player VALUES (2, 9, 27, 65, 1);
INSERT INTO resenha.tournament_player VALUES (2, 16, 34, 64, 1);
INSERT INTO resenha.tournament_player VALUES (2, 12, 30, 57, 1);
INSERT INTO resenha.tournament_player VALUES (5, 11, 76, 76, NULL);
INSERT INTO resenha.tournament_player VALUES (5, 6, 77, 69, NULL);
INSERT INTO resenha.tournament_player VALUES (5, 1, 73, 71, NULL);
INSERT INTO resenha.tournament_player VALUES (5, 17, 74, 68, NULL);
INSERT INTO resenha.tournament_player VALUES (5, 10, 75, 75, NULL);
INSERT INTO resenha.tournament_player VALUES (5, 12, 79, 77, NULL);
INSERT INTO resenha.tournament_player VALUES (5, 5, 80, 78, NULL);
INSERT INTO resenha.tournament_player VALUES (5, 14, 81, 70, NULL);
INSERT INTO resenha.tournament_player VALUES (5, 16, 84, 72, NULL);
INSERT INTO resenha.tournament_player VALUES (5, 13, 83, 74, NULL);
INSERT INTO resenha.tournament_player VALUES (5, 15, 82, 73, NULL);
INSERT INTO resenha.tournament_player VALUES (5, 8, 78, 79, NULL);



--
-- TOC entry 5203 (class 0 OID 41295)
-- Dependencies: 255
-- Data for Name: group_tournament; Type: TABLE DATA; Schema: resenha; Owner: postgres
--

INSERT INTO resenha.group_tournament VALUES (1, 1, 1, '2026-06-27 13:25:58.125026');
INSERT INTO resenha.group_tournament VALUES (2, 1, 2, '2026-06-27 13:25:58.125026');
INSERT INTO resenha.group_tournament VALUES (3, 1, 3, '2026-06-27 13:25:58.125026');
INSERT INTO resenha.group_tournament VALUES (4, 1, 4, '2026-06-27 13:25:58.125026');
INSERT INTO resenha.group_tournament VALUES (5, 1, 5, '2026-06-27 14:02:38.855311');

--
-- TOC entry 5206 (class 0 OID 41359)
-- Dependencies: 258
-- Data for Name: group_tournament_market_type; Type: TABLE DATA; Schema: resenha; Owner: postgres
--

INSERT INTO resenha.group_tournament_market_type VALUES (1, 'MATCH_RESULT');
INSERT INTO resenha.group_tournament_market_type VALUES (2, 'MATCH_RESULT');
INSERT INTO resenha.group_tournament_market_type VALUES (3, 'MATCH_RESULT');
INSERT INTO resenha.group_tournament_market_type VALUES (4, 'MATCH_RESULT');
INSERT INTO resenha.group_tournament_market_type VALUES (5, 'MATCH_RESULT');

--
-- TOC entry 5205 (class 0 OID 41315)
-- Dependencies: 257
-- Data for Name: tournament_wallet; Type: TABLE DATA; Schema: resenha; Owner: postgres
--

INSERT INTO resenha.tournament_wallet VALUES (1, 1, 1, 40.00, 40.00, '2026-06-27 13:25:58.125026');
INSERT INTO resenha.tournament_wallet VALUES (2, 2, 1, 40.00, 40.00, '2026-06-27 13:25:58.125026');
INSERT INTO resenha.tournament_wallet VALUES (3, 3, 1, 40.00, 40.00, '2026-06-27 13:25:58.125026');
INSERT INTO resenha.tournament_wallet VALUES (4, 4, 1, 40.00, 40.00, '2026-06-27 13:25:58.125026');
INSERT INTO resenha.tournament_wallet VALUES (5, 5, 1, 0.00, 0.00, '2026-06-27 14:02:38.86164');


-- Data for Name: event; Type: TABLE DATA; Schema: resenha; Owner: postgres
-- Self-referential FKs (next_round_event_id, home_source_event_id, away_source_event_id)
-- are set to NULL on insert and patched via UPDATE below.
--

INSERT INTO resenha.event VALUES (7, 3, 11, 5, 1059.52, 13, 1031.99, '2025-07-20 01:00:46.614', 'COMPLETED', 4, 0, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (6, 3, 12, 11, 1023.45, 5, 1077.20, '2025-07-20 01:01:17.455', 'COMPLETED', 2, 1, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (5, 4, 13, 9, 967.88, 17, 931.20, '2025-07-20 01:04:07.954', 'COMPLETED', 0, 3, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (4, 4, 13, 8, 983.82, 14, 945.10, '2025-07-20 01:04:25.179', 'COMPLETED', 4, 1, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (3, 4, 14, 2, 999.76, 17, 948.88, '2025-07-20 01:05:52.282', 'COMPLETED', 2, 4, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (2, 4, 14, 8, 998.04, 10, 980.40, '2025-07-20 01:06:11.25', 'COMPLETED', 2, 2, true, false, 3, 0, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (1, 4, 15, 17, 967.21, 8, 1013.23, '2025-07-20 01:06:43.53', 'COMPLETED', 3, 2, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (48, 5, 16, 10, 965.21, 1, 999.20, NULL, 'COMPLETED', 3, 0, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (49, 5, 16, 15, 1012.15, 16, 983.06, NULL, 'COMPLETED', 2, 1, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (50, 5, 16, 8, 995.12, 13, 1014.31, NULL, 'COMPLETED', 1, 2, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (55, 5, 16, 14, 930.88, 17, 985.32, NULL, 'COMPLETED', 1, 3, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (56, 5, 17, 6, 1087.78, 10, 982.77, NULL, 'COMPLETED', 0, 2, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (57, 5, 17, 13, 1029.43, 5, 1051.36, NULL, 'COMPLETED', 0, 7, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (58, 5, 17, 11, 1049.29, 15, 1026.81, NULL, 'COMPLETED', 2, 0, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (59, 5, 17, 12, 1029.89, 17, 998.83, NULL, 'COMPLETED', 0, 2, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (60, 5, 18, 11, 1067.25, 5, 1069.35, NULL, 'COMPLETED', 0, 2, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (61, 5, 18, 17, 1019.74, 10, 1007.60, NULL, 'COMPLETED', 2, 1, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (62, 5, 19, 5, 1091.61, 17, 1041.36, NULL, 'COMPLETED', 2, 0, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (46, 1, 1, 6, 1000.00, 18, 1000.00, '2025-07-14 21:47:13.876', 'COMPLETED', 3, 1, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (45, 1, 1, 13, 1000.00, 7, 1000.00, '2025-07-14 21:47:30.957', 'COMPLETED', 3, 1, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (44, 1, 1, 4, 1000.00, 8, 1000.00, '2025-07-14 21:47:50.074', 'COMPLETED', 2, 1, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (43, 1, 2, 6, 1016.00, 17, 1016.00, '2025-07-14 21:48:01.366', 'COMPLETED', 5, 0, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (47, 1, 1, 17, 1000.00, 3, 1000.00, '2025-07-14 21:46:27.928', 'COMPLETED', 1, 0, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (42, 1, 2, 4, 1016.00, 13, 1016.00, '2025-07-14 21:49:55.616', 'COMPLETED', 3, 1, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (41, 1, 4, 13, 996.80, 17, 996.80, '2025-07-14 21:50:25.493', 'COMPLETED', 2, 0, true, false, NULL, NULL, NULL, NULL, NULL, true, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (40, 1, 3, 6, 1035.20, 4, 1035.20, '2025-07-14 21:50:39.394', 'COMPLETED', 3, 3, true, false, 3, 0, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (39, 2, 5, 15, 1000.00, 14, 1000.00, '2025-07-14 21:52:05.629', 'COMPLETED', 2, 0, false, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (38, 2, 5, 5, 1000.00, 12, 1000.00, '2025-07-14 21:52:44.807', 'COMPLETED', 4, 0, false, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (37, 2, 5, 8, 984.00, 16, 1000.00, '2025-07-14 21:52:53.242', 'COMPLETED', 1, 0, false, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (36, 2, 5, 2, 1000.00, 17, 977.60, '2025-07-14 21:53:04.087', 'COMPLETED', 0, 0, false, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (35, 2, 5, 1, 1000.00, 9, 1000.00, '2025-07-14 21:53:16.073', 'COMPLETED', 2, 1, false, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (34, 2, 5, 16, 983.26, 17, 978.63, '2025-07-14 21:53:27.515', 'COMPLETED', 1, 0, false, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (33, 2, 5, 12, 984.00, 8, 1000.74, '2025-07-14 21:53:36.573', 'COMPLETED', 1, 0, false, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (32, 2, 5, 5, 1016.00, 15, 1016.00, '2025-07-14 21:53:50.984', 'COMPLETED', 3, 0, false, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (29, 2, 5, 8, 983.97, 15, 1000.00, '2025-07-14 21:54:21.557', 'COMPLETED', 1, 0, false, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (28, 2, 5, 12, 1000.77, 17, 962.84, '2025-07-14 21:54:37.819', 'COMPLETED', 2, 0, false, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (27, 2, 5, 2, 998.97, 1, 1016.00, '2025-07-14 21:54:46.71', 'COMPLETED', 2, 0, false, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (26, 2, 5, 1, 999.22, 16, 999.05, '2025-07-14 21:54:57.499', 'COMPLETED', 2, 1, false, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (25, 2, 5, 15, 983.26, 17, 948.58, '2025-07-14 21:55:11.75', 'COMPLETED', 1, 0, false, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (19, 2, 6, 12, 1015.03, 1, 1015.21, '2025-07-14 21:56:49.246', 'COMPLETED', 6, 0, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (9, 3, 10, 13, 1016.00, 2, 1015.75, '2025-07-20 01:00:02.469', 'COMPLETED', 2, 0, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (31, 2, 5, 6, 1057.60, 14, 984.00, '2025-07-14 21:53:59.085', 'COMPLETED', 0, 0, false, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (30, 2, 5, 5, 1032.00, 6, 1054.26, '2025-07-14 21:54:06.635', 'COMPLETED', 2, 0, false, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (24, 2, 5, 6, 1037.24, 8, 1000.71, '2025-07-14 21:55:27.009', 'COMPLETED', 3, 1, false, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (23, 2, 5, 5, 1049.02, 14, 987.34, '2025-07-14 21:55:39.681', 'COMPLETED', 3, 0, false, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (22, 2, 6, 15, 997.67, 14, 974.15, '2025-07-14 21:56:06.535', 'COMPLETED', 4, 2, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (21, 2, 6, 8, 986.39, 17, 934.17, '2025-07-14 21:56:26.273', 'COMPLETED', 2, 2, true, false, 3, 0, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (20, 2, 6, 6, 1051.56, 5, 1062.21, '2025-07-14 21:56:37.309', 'COMPLETED', 1, 1, true, false, 3, 0, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (18, 2, 7, 15, 1012.59, 8, 1000.00, '2025-07-14 21:57:29.297', 'COMPLETED', 2, 0, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (17, 2, 7, 6, 1068.05, 12, 1031.04, '2025-07-14 21:57:42.404', 'COMPLETED', 1, 1, true, false, 3, 0, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (16, 2, 8, 6, 1085.21, 15, 1031.09, '2025-07-14 21:58:05.579', 'COMPLETED', 3, 1, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (15, 3, 9, 17, 920.56, 10, 1000.00, '2025-07-20 00:55:33.613', 'COMPLETED', 1, 0, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (14, 3, 9, 11, 1000.00, 14, 959.23, '2025-07-20 00:56:01.169', 'COMPLETED', 5, 0, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (13, 3, 9, 8, 981.50, 9, 984.00, '2025-07-20 00:56:09.036', 'COMPLETED', 3, 0, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (12, 3, 10, 6, 1104.15, 17, 940.16, '2025-07-20 00:57:11.713', 'COMPLETED', 1, 0, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (11, 3, 10, 12, 1013.88, 11, 1014.13, '2025-07-20 00:59:04.545', 'COMPLETED', 4, 3, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (10, 3, 10, 5, 1045.72, 8, 997.62, '2025-07-20 00:59:45.497', 'COMPLETED', 1, 0, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);
INSERT INTO resenha.event VALUES (8, 3, 11, 6, 1113.11, 11, 998.12, '2025-07-20 01:00:29.646', 'COMPLETED', 2, 3, true, false, NULL, NULL, NULL, NULL, NULL, false, NULL, NULL, NULL);

-- Patch self-referential FKs (next_round_event_id, home_source_event_id, away_source_event_id)
UPDATE resenha.event SET next_round_event_id = 56 WHERE id = 48;
UPDATE resenha.event SET next_round_event_id = 56 WHERE id = 49;
UPDATE resenha.event SET next_round_event_id = 57 WHERE id = 50;
UPDATE resenha.event SET next_round_event_id = 59 WHERE id = 55;
UPDATE resenha.event SET next_round_event_id = 60, away_source_event_id = 48 WHERE id = 56;
UPDATE resenha.event SET next_round_event_id = 60 WHERE id = 57;
UPDATE resenha.event SET next_round_event_id = 61 WHERE id = 58;
UPDATE resenha.event SET next_round_event_id = 61 WHERE id = 59;
UPDATE resenha.event SET next_round_event_id = 62 WHERE id = 60;
UPDATE resenha.event SET next_round_event_id = 62 WHERE id = 61;
UPDATE resenha.event SET home_source_event_id = 60, away_source_event_id = 61 WHERE id = 62;

--
-- TOC entry 5182 (class 0 OID 41034)
-- Dependencies: 234
-- Data for Name: market; Type: TABLE DATA; Schema: resenha; Owner: postgres
--

INSERT INTO resenha.market VALUES (1, 1, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (2, 2, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (3, 3, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (4, 4, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (5, 5, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (6, 6, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (7, 7, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (8, 8, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (9, 9, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (10, 10, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (11, 11, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (12, 12, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (13, 13, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (14, 14, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (15, 15, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (16, 16, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (17, 17, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (18, 18, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (19, 19, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (20, 20, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (21, 21, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (22, 22, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (23, 23, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (24, 24, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (25, 25, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (26, 26, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (27, 27, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (28, 28, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (29, 29, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (30, 30, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (31, 31, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (32, 32, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (33, 33, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (34, 34, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (35, 35, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (36, 36, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (37, 37, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (38, 38, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (39, 39, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (40, 40, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (41, 41, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (42, 42, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (43, 43, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (44, 44, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (45, 45, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (46, 46, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (47, 47, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (48, 55, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (49, 48, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (50, 49, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (52, 50, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (51, 56, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (54, 58, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (53, 57, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (56, 59, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (57, 61, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (55, 60, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');
INSERT INTO resenha.market VALUES (58, 62, 'Resultado Final', 'CLOSED', 'MATCH_RESULT');

--
-- TOC entry 5184 (class 0 OID 41046)
-- Dependencies: 236
-- Data for Name: outcome; Type: TABLE DATA; Schema: resenha; Owner: postgres
--

INSERT INTO resenha.outcome VALUES (1, 1, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (2, 1, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (3, 2, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (4, 2, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (5, 3, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (6, 3, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (7, 4, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (8, 4, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (9, 5, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (10, 5, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (11, 6, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (12, 6, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (13, 7, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (14, 7, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (15, 8, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (16, 8, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (17, 9, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (18, 9, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (19, 10, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (20, 10, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (21, 11, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (22, 11, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (23, 12, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (24, 12, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (25, 13, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (26, 13, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (27, 14, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (28, 14, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (29, 15, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (30, 15, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (31, 16, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (32, 16, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (33, 17, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (34, 17, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (35, 18, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (36, 18, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (37, 19, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (38, 19, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (39, 20, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (40, 20, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (41, 21, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (42, 21, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (43, 22, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (44, 22, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (45, 23, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (46, 23, 'Empate', 1.05);
INSERT INTO resenha.outcome VALUES (47, 23, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (48, 24, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (49, 24, 'Empate', 1.05);
INSERT INTO resenha.outcome VALUES (50, 24, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (51, 25, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (52, 25, 'Empate', 1.05);
INSERT INTO resenha.outcome VALUES (53, 25, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (54, 26, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (55, 26, 'Empate', 1.05);
INSERT INTO resenha.outcome VALUES (56, 26, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (57, 27, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (58, 27, 'Empate', 1.05);
INSERT INTO resenha.outcome VALUES (59, 27, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (60, 28, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (61, 28, 'Empate', 1.05);
INSERT INTO resenha.outcome VALUES (62, 28, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (63, 29, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (64, 29, 'Empate', 1.05);
INSERT INTO resenha.outcome VALUES (65, 29, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (66, 30, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (67, 30, 'Empate', 1.05);
INSERT INTO resenha.outcome VALUES (68, 30, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (69, 31, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (70, 31, 'Empate', 1.05);
INSERT INTO resenha.outcome VALUES (71, 31, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (72, 32, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (73, 32, 'Empate', 1.05);
INSERT INTO resenha.outcome VALUES (74, 32, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (75, 33, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (76, 33, 'Empate', 1.05);
INSERT INTO resenha.outcome VALUES (77, 33, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (78, 34, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (79, 34, 'Empate', 1.05);
INSERT INTO resenha.outcome VALUES (80, 34, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (81, 35, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (82, 35, 'Empate', 1.05);
INSERT INTO resenha.outcome VALUES (83, 35, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (84, 36, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (85, 36, 'Empate', 1.05);
INSERT INTO resenha.outcome VALUES (86, 36, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (87, 37, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (88, 37, 'Empate', 1.05);
INSERT INTO resenha.outcome VALUES (89, 37, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (90, 38, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (91, 38, 'Empate', 1.05);
INSERT INTO resenha.outcome VALUES (92, 38, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (93, 39, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (94, 39, 'Empate', 1.05);
INSERT INTO resenha.outcome VALUES (95, 39, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (96, 40, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (97, 40, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (98, 41, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (99, 41, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (100, 42, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (101, 42, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (102, 43, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (103, 43, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (104, 44, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (105, 44, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (106, 45, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (107, 45, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (108, 46, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (109, 46, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (110, 47, 'Vitória Casa', 1.05);
INSERT INTO resenha.outcome VALUES (111, 47, 'Vitória Fora', 1.05);
INSERT INTO resenha.outcome VALUES (112, 48, 'Vitória Casa', 3.55);
INSERT INTO resenha.outcome VALUES (113, 48, 'Empate', 4.56);
INSERT INTO resenha.outcome VALUES (114, 48, 'Vitória Fora', 2.01);
INSERT INTO resenha.outcome VALUES (115, 49, 'Vitória Casa', 2.93);
INSERT INTO resenha.outcome VALUES (116, 49, 'Empate', 3.77);
INSERT INTO resenha.outcome VALUES (117, 49, 'Vitória Fora', 2.54);
INSERT INTO resenha.outcome VALUES (118, 50, 'Vitória Casa', 2.43);
INSERT INTO resenha.outcome VALUES (119, 50, 'Empate', 3.88);
INSERT INTO resenha.outcome VALUES (120, 50, 'Vitória Fora', 3.02);
INSERT INTO resenha.outcome VALUES (121, 51, 'Vitória Casa', 3.09);
INSERT INTO resenha.outcome VALUES (122, 51, 'Empate', 3.97);
INSERT INTO resenha.outcome VALUES (123, 51, 'Vitória Fora', 2.35);
INSERT INTO resenha.outcome VALUES (124, 52, 'Vitória Casa', 3.05);
INSERT INTO resenha.outcome VALUES (125, 52, 'Empate', 3.93);
INSERT INTO resenha.outcome VALUES (126, 52, 'Vitória Fora', 2.39);
INSERT INTO resenha.outcome VALUES (127, 53, 'Vitória Casa', 3.54);
INSERT INTO resenha.outcome VALUES (128, 53, 'Empate', 4.55);
INSERT INTO resenha.outcome VALUES (129, 53, 'Vitória Fora', 2.01);
INSERT INTO resenha.outcome VALUES (130, 54, 'Vitória Casa', 2.42);
INSERT INTO resenha.outcome VALUES (131, 54, 'Empate', 3.89);
INSERT INTO resenha.outcome VALUES (132, 54, 'Vitória Fora', 3.03);
INSERT INTO resenha.outcome VALUES (136, 56, 'Vitória Casa', 2.05);
INSERT INTO resenha.outcome VALUES (137, 56, 'Empate', 4.45);
INSERT INTO resenha.outcome VALUES (138, 56, 'Vitória Fora', 3.46);
INSERT INTO resenha.outcome VALUES (139, 57, 'Vitória Casa', 2.26);
INSERT INTO resenha.outcome VALUES (140, 57, 'Empate', 4.10);
INSERT INTO resenha.outcome VALUES (141, 57, 'Vitória Fora', 3.19);
INSERT INTO resenha.outcome VALUES (133, 55, 'Vitória Casa', 2.47);
INSERT INTO resenha.outcome VALUES (134, 55, 'Empate', 4.22);
INSERT INTO resenha.outcome VALUES (135, 55, 'Vitória Fora', 2.79);
INSERT INTO resenha.outcome VALUES (142, 58, 'Vitória Casa', 2.08);
INSERT INTO resenha.outcome VALUES (143, 58, 'Empate', 4.41);
INSERT INTO resenha.outcome VALUES (144, 58, 'Vitória Fora', 3.43);

--
-- TOC entry 5171 (class 0 OID 40938)
-- Dependencies: 223
-- Data for Name: transaction; Type: TABLE DATA; Schema: resenha; Owner: postgres
--

-- Reset all BIGSERIAL sequences to MAX(id) so GenerationType.IDENTITY won't conflict
-- with the explicitly-inserted IDs above.

SELECT setval('resenha.groups_id_seq',             (SELECT COALESCE(MAX(id), 1) FROM resenha.groups));
SELECT setval('resenha.team_id_seq',               (SELECT COALESCE(MAX(id), 1) FROM resenha.team));
SELECT setval('resenha.player_id_seq',             (SELECT COALESCE(MAX(id), 1) FROM resenha.player));
SELECT setval('resenha.tournament_id_seq',         (SELECT COALESCE(MAX(id), 1) FROM resenha.tournament));
SELECT setval('resenha.tournament_round_id_seq',   (SELECT COALESCE(MAX(id), 1) FROM resenha.tournament_round));
SELECT setval('resenha.tournament_player_id_seq',  (SELECT COALESCE(MAX(id), 1) FROM resenha.tournament_player));

SELECT setval('resenha.group_tournament_id_seq',   (SELECT COALESCE(MAX(id), 1) FROM resenha.group_tournament));
SELECT setval('resenha.tournament_wallet_id_seq',  (SELECT COALESCE(MAX(id), 1) FROM resenha.tournament_wallet));
SELECT setval('resenha.event_id_seq',              (SELECT COALESCE(MAX(id), 1) FROM resenha.event));
SELECT setval('resenha.market_id_seq',             (SELECT COALESCE(MAX(id), 1) FROM resenha.market));
SELECT setval('resenha.outcome_id_seq',            (SELECT COALESCE(MAX(id), 1) FROM resenha.outcome));
SELECT setval('resenha.transaction_id_seq',        (SELECT COALESCE(MAX(id), 1) FROM resenha.transaction));

