# azani_db_dml.sql  (Python cell for Colab: `conn` and `cur` must already be open)
#
# Run after azani_db_ddl.sql. Clears every table first, so you can run it more than once.
# Every figure below comes from the SCO200 brief. The sample data is a snapshot taken on 2025-04-30.
# Institution names marked "fictional" are invented so that the primary and junior categories hold rows.

from datetime import date, timedelta
from decimal import Decimal, ROUND_HALF_UP

# Figures from the brief. The Java Fees class holds the same values.
REGISTRATION_FEE = Decimal("8500.00")
INSTALLATION_FEE = Decimal("10000.00")
PC_PRICE         = Decimal("40000.00")
RECONNECTION_FEE = Decimal("1000.00")
FINE_RATE        = Decimal("0.15")
UPGRADE_DISCOUNT = Decimal("0.10")

def money(x):
    """Two decimals, HALF_UP, matching the rounding rule recorded for the report."""
    return Decimal(x).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)

# ---------------------------------------------------------------- 1. clear every table
cur.execute("SET FOREIGN_KEY_CHECKS = 0")
for table in ["disconnections", "overdue_fines", "payments", "upgrades", "subscriptions",
              "bandwidth_plans", "installations", "equipment_orders", "lan_node_tiers",
              "readiness_assessments", "contact_persons", "institutions"]:
    cur.execute(f"TRUNCATE TABLE {table}")
cur.execute("SET FOREIGN_KEY_CHECKS = 1")

# ---------------------------------------------------------------- 2. price lists (brief Tables 1 and 2)
cur.executemany(
    "INSERT INTO bandwidth_plans (mbps, monthly_cost) VALUES (%s, %s)",
    [(4, "1200.00"), (10, "2000.00"), (20, "3500.00"), (25, "4000.00"), (50, "7000.00")])
cur.executemany(
    "INSERT INTO lan_node_tiers (min_nodes, max_nodes, cost) VALUES (%s, %s, %s)",
    [(2, 10, "10000.00"), (11, 20, "20000.00"), (21, 40, "30000.00"), (41, 100, "40000.00")])

cur.execute("SELECT mbps, plan_id, monthly_cost FROM bandwidth_plans")
PLANS = {mbps: (plan_id, cost) for mbps, plan_id, cost in cur.fetchall()}
cur.execute("SELECT min_nodes, tier_id, cost FROM lan_node_tiers")
TIERS = {min_nodes: (tier_id, cost) for min_nodes, tier_id, cost in cur.fetchall()}

# ---------------------------------------------------------------- 3. institutions
# name, category, address, users, has_computers, has_lan, starting Mbps, contact, email
# Universities are filed under 'college', the closest category in the brief.
INSTITUTIONS = [
    ("University of Nairobi", "college", "University Way, Nairobi", 1200, True, True, 50, "Grace Wanjiku", "grace.wanjiku@uonbi.ac.ke"),
    ("Kenyatta University", "college", "Kahawa, Nairobi", 1200, True, True, 50, "Peter Mwangi", "p.mwangi@ku.ac.ke"),
    ("Jomo Kenyatta University of Agriculture and Technology", "college", "Juja, Kiambu County", 1200, True, True, 50, "Faith Njeri", "faith.njeri@jkuat.ac.ke"),
    ("Egerton University", "college", "Njoro, Nakuru County", 1200, True, True, 50, "Daniel Kiptoo", "daniel.kiptoo@egerton.ac.ke"),
    ("Maseno University", "college", "Maseno, Kisumu County", 1200, True, True, 50, "Achieng Otieno", "a.otieno@maseno.ac.ke"),
    ("Moi University", "college", "Kesses, Uasin Gishu County", 1200, True, True, 50, "Brian Kiplagat", "b.kiplagat@mu.ac.ke"),
    ("University of Eldoret", "college", "Kapseret, Uasin Gishu County", 1200, True, True, 50, "Janet Chebet", "janet.chebet@uoeld.ac.ke"),
    ("Pwani University", "college", "Kilifi, Kilifi County", 1200, True, True, 50, "Salim Juma", "salim.juma@pu.ac.ke"),
    ("Technical University of Kenya", "college", "Haile Selassie Avenue, Nairobi", 1200, True, True, 50, "Mercy Akinyi", "mercy.akinyi@tukenya.ac.ke"),
    ("Strathmore University", "college", "Madaraka Estate, Nairobi", 1200, True, True, 50, "Kevin Mutua", "kevin.mutua@strathmore.edu"),
    ("Alliance High School", "senior", "Kikuyu, Kiambu County", 450, False, False, 10, "Samuel Kariuki", "s.kariuki@alliancehighschool.ac.ke"),
    ("The Kenya High School", "senior", "Kileleshwa, Nairobi", 450, True, True, 10, "Lydia Wambui", "lydia.wambui@kenyahigh.ac.ke"),
    ("Mangu High School", "senior", "Thika, Kiambu County", 450, True, True, 10, "Joseph Maina", "j.maina@manguhigh.com"),
    ("Starehe Boys Centre and School", "senior", "Nairobi", 450, True, True, 10, "Esther Naliaka", "esther.naliaka@stareheboys.org"),
    ("Loreto High School Limuru", "senior", "Limuru, Kiambu County", 450, True, True, 10, "Mary Wairimu", "mary.wairimu@loreto-limuru.sc.ke"),
    ("Maseno School", "senior", "Maseno, Kisumu County", 450, True, True, 10, "George Ouma", "g.ouma@masenoschool.sc.ke"),
    ("Kapsabet Boys High School", "senior", "Kapsabet, Nandi County", 450, True, True, 10, "David Sang", "d.sang@kapsabetboys.sc.ke"),
    ("Nakuru High School", "senior", "Nakuru, Nakuru County", 450, True, True, 10, "Alice Chepngeno", "alice.chepngeno@nakuruhigh.sc.ke"),
    ("Kenya Medical Training College", "college", "Old Mbagathi Road, Nairobi", 700, True, False, 20, "Rose Atieno", "rose.atieno@kmtc.ac.ke"),
    ("Kenya Institute of Highways and Building Technology", "college", "Ngong Road, Nairobi", 700, True, True, 20, "Martin Karanja", "m.karanja@kihbt.ac.ke"),
    ("Kenya Utalii College", "college", "Thika Road, Nairobi", 700, True, True, 20, "Hassan Noor", "h.noor@utalii.ac.ke"),
    ("Nairobi Technical Training Institute", "college", "Ngara, Nairobi", 700, True, True, 20, "Lucy Nyambura", "lucy.nyambura@ntti.ac.ke"),
    ("Rift Valley Institute of Science and Technology", "college", "Nakuru, Nakuru County", 700, True, True, 20, "Peter Cheruiyot", "p.cheruiyot@rvist.ac.ke"),
    ("Kisumu National Polytechnic", "college", "Kisumu, Kisumu County", 700, True, True, 20, "Susan Auma", "susan.auma@kisumupoly.ac.ke"),
    ("Mombasa Technical Training Institute", "college", "Mombasa, Mombasa County", 700, False, True, 20, "Ahmed Bakari", "a.bakari@mombasatti.ac.ke"),
    ("Eldoret National Polytechnic", "college", "Eldoret, Uasin Gishu County", 700, True, True, 20, "Emily Jepkosgei", "emily.jepkosgei@tenp.ac.ke"),
    ("Thika Technical Training Institute", "college", "Thika, Kiambu County", 700, True, True, 20, "Patrick Kimani", "p.kimani@thikatechnical.ac.ke"),
    ("Kenya Institute of Mass Communication", "college", "South C, Nairobi", 700, False, False, None, "Naomi Wekesa", "naomi.wekesa@kimc.ac.ke"),
    # Fictional institutions for the empty categories.
    ("Kibera Hill Primary School", "primary", "Kibera, Nairobi", 300, False, False, 4, "Ruth Adhiambo", "ruth.adhiambo@example.org"),
    ("Lakeview Primary School", "primary", "Kisumu, Kisumu County", 320, True, True, 4, "John Odhiambo", "john.odhiambo@example.org"),
    ("Mwatate Junior School", "junior", "Mwatate, Taita Taveta County", 350, True, True, 4, "Zawadi Mwakio", "zawadi.mwakio@example.org"),
    ("Ridgeway Junior School", "junior", "Ruiru, Kiambu County", 280, True, False, None, "Tom Kamau", "tom.kamau@example.org"),
]

# Institutions that have not passed the site visit and have placed no order yet.
PENDING = {"Kenya Institute of Mass Communication", "Ridgeway Junior School"}
SUSPENDED = {"Nakuru High School"}

REGISTERED = {}
inst_rows = []
for i, (name, kind, address, *_rest) in enumerate(INSTITUTIONS):
    REGISTERED[name] = date(2025, 1, 6) + timedelta(days=i)
    status = "inactive" if name in PENDING else ("suspended" if name in SUSPENDED else "active")
    inst_rows.append((name, kind, address, REGISTERED[name], status))
cur.executemany(
    "INSERT INTO institutions (name, type, address, registered_on, status) VALUES (%s, %s, %s, %s, %s)",
    inst_rows)

cur.execute("SELECT name, institution_id FROM institutions")
IDS = dict(cur.fetchall())

# ---------------------------------------------------------------- 4. contact persons
cur.executemany(
    "INSERT INTO contact_persons (institution_id, full_name, phone, email) VALUES (%s, %s, %s, %s)",
    [(IDS[row[0]], row[7], "+254700100%03d" % (i + 1), row[8]) for i, row in enumerate(INSTITUTIONS)])

# ---------------------------------------------------------------- 5. site visits
# is_ready is true only when the institution already has computers and a LAN.
cur.executemany(
    """INSERT INTO readiness_assessments
       (institution_id, visit_date, user_count, has_computers, has_lan, is_ready)
       VALUES (%s, %s, %s, %s, %s, %s)""",
    [(IDS[r[0]], date(2025, 2, 10), r[3], r[4], r[5], r[4] and r[5]) for r in INSTITUTIONS])

# ---------------------------------------------------------------- 6. equipment orders
# name -> (computers bought, min_nodes of the LAN tier bought or None)
ORDERS = {
    "Alliance High School":                 (20, 11),    # no computers, no LAN
    "Kenya Medical Training College":       (0, 21),     # has computers, no LAN
    "Mombasa Technical Training Institute": (15, None),  # has LAN, no computers
    "Kibera Hill Primary School":           (5, 2),      # no computers, no LAN
}
order_rows = []
for name, (qty, tier_min) in ORDERS.items():
    computer_cost = money(PC_PRICE * qty)
    tier_id, lan_cost = TIERS[tier_min] if tier_min else (None, Decimal("0.00"))
    order_rows.append((IDS[name], tier_id, qty, computer_cost, lan_cost,
                       computer_cost + lan_cost, date(2025, 2, 12)))
cur.executemany(
    """INSERT INTO equipment_orders
       (institution_id, tier_id, computer_qty, computer_cost, lan_cost, total_cost, order_date)
       VALUES (%s, %s, %s, %s, %s, %s, %s)""", order_rows)

# ---------------------------------------------------------------- 7. installations
INSTALLED_ON = {}
for row in INSTITUTIONS:
    name = row[0]
    if name not in PENDING:
        INSTALLED_ON[name] = date(2025, 2, 24) if name in ORDERS else date(2025, 2, 20)
cur.executemany(
    "INSERT INTO installations (institution_id, fee, installed_on) VALUES (%s, %s, %s)",
    [(IDS[n], INSTALLATION_FEE, d) for n, d in INSTALLED_ON.items()])

# ---------------------------------------------------------------- 8. subscriptions
START_MBPS = {r[0]: r[6] for r in INSTITUTIONS if r[0] not in PENDING}
cur.executemany(
    "INSERT INTO subscriptions (institution_id, plan_id, start_date, status) VALUES (%s, %s, %s, %s)",
    [(IDS[n], PLANS[m][0], date(2025, 3, 1), "disconnected" if n in SUSPENDED else "active")
     for n, m in START_MBPS.items()])
cur.execute("SELECT institution_id, subscription_id FROM subscriptions")
SUBS = dict(cur.fetchall())

# ---------------------------------------------------------------- 9. upgrades
# The discount is 10 percent of the cost of the bandwidth upgraded to.
UPGRADES = [
    ("Kenya Medical Training College", 20, 50, date(2025, 4, 1)),
    ("Alliance High School",           10, 25, date(2025, 4, 1)),
]
for name, old_mbps, new_mbps, when in UPGRADES:
    cur.execute(
        """INSERT INTO upgrades
           (subscription_id, old_plan_id, new_plan_id, discount_rate, upgraded_on)
           VALUES (%s, %s, %s, %s, %s)""",
        (SUBS[IDS[name]], PLANS[old_mbps][0], PLANS[new_mbps][0], UPGRADE_DISCOUNT, when))
    cur.execute("UPDATE subscriptions SET plan_id = %s WHERE subscription_id = %s",
                (PLANS[new_mbps][0], SUBS[IDS[name]]))

# ---------------------------------------------------------------- 10. payments
payments = []
for i, row in enumerate(INSTITUTIONS):
    name = row[0]
    payments.append((IDS[name], "registration", REGISTRATION_FEE, REGISTERED[name], None))
    if name in INSTALLED_ON:
        payments.append((IDS[name], "installation", INSTALLATION_FEE, INSTALLED_ON[name], None))

# March 2025 bills, at the plan each institution held in March.
LATE_PAID = {"Egerton University": date(2025, 4, 14), "Maseno School": date(2025, 4, 5)}
UNPAID = {"Nakuru High School"}
for i, (name, mbps) in enumerate(START_MBPS.items()):
    if name in UNPAID:
        continue
    paid_on = LATE_PAID.get(name, date(2025, 3, 5) + timedelta(days=i % 20))
    payments.append((IDS[name], "monthly", PLANS[mbps][1], paid_on, "2025-03"))

# April 2025 bills for the two upgraded institutions, at the discounted rate.
for (name, _old, new_mbps, _when), paid_on in zip(UPGRADES, [date(2025, 4, 28), date(2025, 4, 29)]):
    discounted = money(PLANS[new_mbps][1] * (1 - UPGRADE_DISCOUNT))
    payments.append((IDS[name], "monthly", discounted, paid_on, "2025-04"))

# Egerton cleared its bill and fine, then paid the reconnection fee.
payments.append((IDS["Egerton University"], "reconnection", RECONNECTION_FEE, date(2025, 4, 14), "2025-03"))

cur.executemany(
    "INSERT INTO payments (institution_id, payment_type, amount, paid_on, billing_month) VALUES (%s, %s, %s, %s, %s)",
    payments)

# ---------------------------------------------------------------- 11. overdue fines (15 percent, once per bill)
def fine_for(name):
    return money(PLANS[START_MBPS[name]][1] * FINE_RATE)

cur.executemany(
    "INSERT INTO overdue_fines (institution_id, billing_month, fine_amount, settled) VALUES (%s, %s, %s, %s)",
    [(IDS["Egerton University"], "2025-03", fine_for("Egerton University"), True),
     (IDS["Maseno School"],      "2025-03", fine_for("Maseno School"),      True),
     (IDS["Nakuru High School"], "2025-03", fine_for("Nakuru High School"), False)])

# ---------------------------------------------------------------- 12. disconnections
# Both institutions were still unpaid after the 10th of April, so both were disconnected on the 11th.
cur.executemany(
    "INSERT INTO disconnections (institution_id, disconnected_on, reconnected, reconnected_on) VALUES (%s, %s, %s, %s)",
    [(IDS["Egerton University"], date(2025, 4, 11), True, date(2025, 4, 15)),
     (IDS["Nakuru High School"], date(2025, 4, 11), False, None)])

conn.commit()

# ---------------------------------------------------------------- 13. self-check
EXPECTED = {"institutions": 32, "contact_persons": 32, "readiness_assessments": 32,
            "equipment_orders": 4, "installations": 30, "subscriptions": 30,
            "upgrades": 2, "payments": 94, "overdue_fines": 3, "disconnections": 2,
            "bandwidth_plans": 5, "lan_node_tiers": 4}
for table, want in EXPECTED.items():
    cur.execute(f"SELECT COUNT(*) FROM {table}")
    got = cur.fetchone()[0]
    assert got == want, f"{table}: expected {want}, found {got}"
print("Sample data loaded; every row count matches.")