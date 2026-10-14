--
-- PostgreSQL database dump
--

\restrict KypONWZfyFGR5Og8EnExlVfaOyqaPLI78tbqREVxr6jej8uQo42Usbuxon1cEbX

-- Dumped from database version 15.19
-- Dumped by pg_dump version 15.19

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: habit_log; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.habit_log (id, date, status, category_id, user_id) FROM stdin;
1	2026-10-05	DONE	1	1
2	2026-10-05	PARTIAL	2	1
3	2026-10-04	SKIPPED	4	1
4	2026-10-06	DONE	6	1
6	2026-10-06	DONE	3	1
7	2026-10-06	DONE	1	1
9	2026-10-06	DONE	4	1
8	2026-10-06	DONE	2	1
5	2026-10-06	PARTIAL	5	1
10	2026-10-07	DONE	1	1
11	2026-10-07	DONE	3	1
12	2026-10-09	DONE	1	1
\.


--
-- Name: habit_log_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.habit_log_id_seq', 12, true);


--
-- PostgreSQL database dump complete
--

\unrestrict KypONWZfyFGR5Og8EnExlVfaOyqaPLI78tbqREVxr6jej8uQo42Usbuxon1cEbX

