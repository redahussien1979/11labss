"""Generate the 100-question PF Credit Policy exam (HTML), rendered to PDF by build.js."""
import html, re, pathlib

HERE = pathlib.Path(__file__).parent
HEAD = (HERE.parent / "training/src/00_head.html").read_text()
HEAD = HEAD.replace("Personal Finance Credit Policy – Training Guide", "PF Credit Policy – 100 Exam Questions")

EXTRA_CSS = """
<style>
  .qq{border:1px solid var(--line);border-radius:9px;padding:8px 11px 8px;margin:8px 0 10px;break-inside:avoid;background:#fff}
  .qq .qh{display:flex;gap:8px;align-items:center;margin-bottom:4px}
  .qq .qn{font-weight:bold;color:#fff;background:var(--blue);border-radius:6px;padding:1px 8px;font-size:9.5pt}
  .qq .tag{font-size:7.6pt;font-weight:bold;border-radius:9px;padding:1px 7px;color:#fff}
  .t-calc{background:var(--amber)} .t-mcq{background:var(--teal)} .t-case{background:var(--purple)}
  .qq .ref{font-size:7.8pt;color:var(--muted);margin-left:auto}
  .qq ol.opt{list-style:upper-alpha;margin:3px 0 4px;padding-left:22px}
  .qq .aa{background:var(--green-l);border-left:4px solid var(--green);border-radius:6px;padding:5px 9px;margin-top:5px;font-size:9.3pt}
  .qq .aa b.al{color:var(--green)}
  .qq .unsure{background:#fff6c2;border-left:4px solid #e0a800;border-radius:6px;padding:5px 9px;margin-top:5px;font-size:9pt}
  .qq .unsure b{color:#9a6a00}
  .qq .calc{margin:4px 0}
  .legend span{display:inline-block;margin-right:8px}
</style>
"""

SECTIONS = {
 "A": ("b-green",  "Part A", "Definitions, Purpose &amp; Scope (Sections 1–4)"),
 "B": ("b-teal",   "Part B", "Segments &amp; Schemes (Section 5)"),
 "C": ("b-blue",   "Part C", "Liability Consolidation, Settlement &amp; BFI (5.1–5.4)"),
 "D": ("b-purple", "Part D", "Scorecard, Employer Codes &amp; AECB (Notes C–D)"),
 "E": ("b-green",  "Part E", "Minimum Financing Standards (Section 6)"),
 "F": ("b-blue",   "Part F", "Salaried Target Market (7.1)"),
 "G": ("b-amber",  "Part G", "Against TD, Sukuk Packs, Cross Sell, Private Banking (7.2–7.6)"),
 "H": ("b-red",    "Part H", "Finance Payment Relief (7.7 &amp; 8.4)"),
 "I": ("b-teal",   "Part I", "Documentation &amp; General Guidelines (8.1)"),
 "J": ("b-purple", "Part J", "Credit Delegations (8.2) &amp; Self-Employed Grid (8.3)"),
}

# (part, type, ref, question, options|None, answer, unsure|None)
Q = []
def q(part, typ, ref, text, ans, opts=None, unsure=None):
    Q.append((part, typ, ref, text, opts, ans, unsure))

C = lambda s: f'<div class="calc">{s}</div>'

# ---------------- PART A ----------------
q("A","mcq","Sec. 4","A customer holds an Omani passport and works in Dubai. For this policy the customer is:",
  "<b class='al'>B</b> – GCC nationals are categorised as Expat (definition of EXPAT).",
  ["UAE National","Expat","GCC resident – treated as UAE National","Non-resident"])
q("A","mcq","Sec. 4","Which statement about DIB Staff is correct?",
  "<b class='al'>C</b> – DIB staff are ineligible to receive financing under the pre-approved segment for any product; their income/liability data is restricted.",
  ["They are processed like Non-TML customers","They can be offered pre-approved PF if tagged in Core","They are ineligible for financing under the pre-approved segment for any product","Their DBR is calculated by RCD"])
q("A","mcq","Sec. 4","An application fails one policy-engine parameter and the deviation is minor as per the delegation matrix. It is a:",
  "<b class='al'>B</b> – Level 2 = minor deviation; Level 1 = no deviation; Level 3 = more severe than Level 2.",
  ["Level 1 case","Level 2 case","Level 3 case","Policy-exception (CCO) case"])
q("A","mcq","Sec. 3","Which of the following is NOT covered by this Policy?",
  "<b class='al'>D</b> – The policy does not cover underwriting parameters for stressed customers.",
  ["Repeat finance deals underwritten by RCD","Applications submitted via digital channels","Fresh finance for salaried customers with verifiable income","Underwriting parameters for stressed customers"])
q("A","mcq","Sec. 1 &amp; 4","Sharia principles for this policy are approved by the:",
  "<b class='al'>A</b> – ISSC = Internal Sharia Supervision Committee.",
  ["ISSC","HSA","BRCGC","RCPPM"])
q("A","mcq","Sec. 4","In this policy \"PDC\" refers to:",
  "<b class='al'>C</b> – PDC is the historical name of the segment of customers having salary transfer relationship with <u>other</u> banks.",
  ["Customers who must give post-dated cheques for every EMI","Private banking deposit customers","Customers whose salary is transferred to another bank","Pensioners with DIB pension credit"])

# ---------------- PART B ----------------
q("B","mcq","Sec. 5 – STL","Which employer categories qualify for the STL segment (salary with DIB + STL in DIB format)?",
  "<b class='al'>B</b> – TML category A, B &amp; C (plus DIB staff treated as STL Cat A, Wajaha salaried and Ayaan ≥ 12 months).",
  ["A and B only","A, B and C","A, B, C and D","Any TML or Non-TML employer"])
q("B","case","Sec. 5 – STL waiver","Customer works for a TML <b>Cat B</b> employer, has provided STL in DIB format, but no salary has been credited to DIB yet. HR-CPV confirms next salary will be credited to DIB. Can the \"first salary in DIB\" requirement be waived?",
  "<b class='al'>Yes</b> – criterion (a) Confirmed Future Credit. Waivers apply only to Cat A, B &amp; C with STL provided.")
q("B","calc","Sec. 5 – STL waiver (d)","TML Cat A customer changed job and does not have 3 salary credits in the last 90 days. Last salary from old employer: <b>15 February</b>; latest salary from new employer: <b>10 June</b> (non-leap year). Can the case be accepted under the New-to-Country policy?",
  C("15 Feb → 28 Feb = 13 days\nMarch 31 + April 30 + May 31 = 92 days\n1 Jun → 10 Jun = 10 days\nGap = 13 + 92 + 10 = 115 days  ≤ 121 days")+"<b class='al'>Yes</b> – the gap is within 121 days.")
q("B","calc","Sec. 5 – STL waiver (d)","Same rule as previous question. Last salary old employer: <b>20 January</b>; latest salary new employer: <b>25 May</b> (non-leap year). Accepted?",
  C("20 Jan → 31 Jan = 11 days\nFeb 28 + Mar 31 + Apr 30 = 89 days\n1 May → 25 May = 25 days\nGap = 11 + 89 + 25 = 125 days  > 121 days")+"<b class='al'>No</b> – gap exceeds 121 days, so waiver (d) does not apply.")
q("B","case","Sec. 5 – Salaried/Privileged","Non-TML employee. Salary credits: March – DIB, April – Bank X, May – Bank X. Does the customer meet the Salaried/Privileged salary-transfer requirement?",
  "<b class='al'>No</b> – 3 salaries in total are present, but at least 1 in the <u>most recent month</u> must be credited to DIB. May (latest) went to Bank X.")
q("B","mcq","Sec. 5 – PDC","A PDC customer working for a Non-TML company requests Liability Consolidation. The decision is:",
  "<b class='al'>C</b> – \"Liability Consolidation for Non TML under PDC is not allowed.\"",
  ["Allowed after 12 months","Allowed after 3 instalments","Not allowed","Allowed only with Head of Card approval"])
q("B","mcq","Sec. 5 – Self-employed","Since when have Self-Employed applicants been ineligible for Personal Finance?",
  "<b class='al'>A</b> – stopped in October 2022 (Annexure 8.3 parameters are only for cases RCD accepts under deviation).",
  ["October 2022","November 2022","January 2023","The segment is still open for UAE Nationals"])
q("B","mcq","Sec. 5 &amp; 7.2","PF against an investment deposit: which deposit currency is acceptable?",
  "<b class='al'>B</b> – the deposit can be accepted in AED or USD only.",
  ["AED, USD or EUR","AED or USD","Any currency held with DIB","AED only"])
q("B","mcq","Sec. 5 &amp; 7.2","A customer wants PF against an FD that belongs to his brother (third party). What must be obtained?",
  "<b class='al'>D</b> – personal guarantee of the third party.",
  ["Nothing extra","A NOC from the brother's employer","A salary transfer letter from the brother","Personal guarantee of the third party"])
q("B","case","Sec. 5 – STL item 4","An Ayaan Exclusive Banking customer with 14 months relationship submits a PF application through a normal retail branch (not through the Ayaan RM). Which parameters apply?",
  "<b class='al'>Normal segmental (retail) policy parameters.</b> Only if submitted through the respective unit RM is it processed as Private Banking segment.")
q("B","mcq","Sec. 5 – Sukuk Pack","Which statement about the Sukuk Pack segment is correct?",
  "<b class='al'>A</b> – AF and PF applied at the same time under two applications/contracts using the same KYC and income documents; non-scorecard segment.",
  ["Two separate applications, same KYC/income documents, non-scorecard","One combined contract, scorecard enabled","Only for Home Finance customers","Requires fresh AECB for the PF"])

# ---------------- PART C ----------------
q("C","case","5.1.3","TML Cat A STL customer booked a PF <b>8 months</b> ago at <b>18x</b> salary. Salary unchanged. Can incremental finance be given under Liability Consolidation now?",
  "<b class='al'>Yes</b> – the customer did not avail 20 multiples in the previous finance, so incremental finance is allowed before 12 months (subject to all other norms).")
q("C","calc","5.1.3","Customer availed <b>20x</b> 5 months ago on salary AED 9,800. Current salary: (i) AED 9,950 (ii) AED 10,050. In which case does the income increase alone allow LC before 12 months?",
  C("(i)  9,950 − 9,800 = 150  → less than AED 200 ✘\n(ii) 10,050 − 9,800 = 250 → at least AED 200 ✔")+"<b class='al'>Case (ii) only.</b>")
q("C","case","5.1.3","Non-TML (DIB salaried) customer, PF booked 10 months ago at 15x, paid <b>2</b> instalments. Eligible for LC before 12 months?",
  "<b class='al'>Not yet</b> – for Non-TML companies the applicant must pay at least <b>3</b> instalments to qualify for LC before 12 months.")
q("C","mcq","5.1.1","Which products can be consolidated under Liability Consolidation?",
  "<b class='al'>C</b> – only PF &amp; AF; CC consolidation is subject to Head of Card approval.",
  ["PF only","PF, AF and CC freely","PF and AF; CC only with Head of Card approval","Any retail product"])
q("C","mcq","5.2.2","Liability Settlement is currently permitted for:",
  "<b class='al'>B</b> – TML A, B, C, D and DIB Staff. Non-TML is approved but can be implemented only once system readiness is confirmed.",
  ["TML A, B, C only","TML A, B, C, D and DIB Staff","All employers including Non-TML","DIB staff only"])
q("C","case","5.2.3 / 5.2.4","After a Liability Settlement, the customer (a) wants an Auto Finance before the first salary reaches DIB, and (b) wants a covered card. What applies?",
  "(a) <b class='al'>Wait</b> – PF/AF may be applied for once the first salary is credited to DIB.<br>(b) Covered card upon submission of <b>STL + Clearance Letters from all banks</b> where liabilities were settled.")
q("C","calc","5.4","BFI employee (Expat), DIB salary transfer <b>with</b> STL &amp; EOSB commitment, salary AED 9,500, applying for Fresh PF. Does the customer meet minimum income?",
  C("Expat minimum (with STL &amp; EOSB) = AED 10,000\n9,500 &lt; 10,000")+"<b class='al'>No.</b> (Max exposure in this row would have been AED 3 million.)")
q("C","calc","5.4","BFI employee (Expat) at a TML bank, <b>no</b> STL commitment, salary AED 18,000. (a) Fresh PF? (b) Liability Consolidation? (c) Max exposure?",
  C("Fresh minimum = 25K → 18,000 &lt; 25,000 ✘\nLC minimum    = 15K → 18,000 ≥ 15,000 ✔")+"(a) <b class='al'>Not eligible</b> (b) <b class='al'>Eligible</b> (c) <b class='al'>AED 300,000</b> (TML).")
q("C","calc","5.4","BFI employee (UAE National) with <b>no</b> STL commitment, salary credited to <b>another bank</b> (PDC), salary AED 4,200. Eligible on income?",
  C("UAEN minimum = 3K for STL &amp; 5K for PDC → PDC applies → 4,200 &lt; 5,000")+"<b class='al'>No.</b>")
q("C","mcq","5.4 Notes","Eligible BFIs are assigned which TML category?",
  "<b class='al'>B</b> – Category B (also set in SAS DMS to avoid manual overrides).",
  ["A","B","C","D"])
q("C","mcq","5.4 Notes / 8.1","Outsourced staff of a bank apply for PF. The decision is:",
  "<b class='al'>D</b> – strictly restricted across all segments.",
  ["Allowed as Non-TML","Allowed with STL","Allowed with EOSB commitment","Restricted in all segments"])

# ---------------- PART D ----------------
q("D","calc","Note C-III","Both customers meet every policy parameter on the <b>digital journey</b>: (a) Expat requesting AED 180,000 (b) UAE National requesting AED 480,000. Which can be auto-passed?",
  C("Expat ceiling  175,000 → 180,000 &gt; 175,000 ✘\nUAEN ceiling   500,000 → 480,000 ≤ 500,000 ✔")+"<b class='al'>Only (b).</b> (No auto-pass ceiling on the conventional journey.)")
q("D","mcq","Note C-V","A case is Straight Pass (STP) when:",
  "<b class='al'>B</b> – system flags it Auto Pass AND the CPV flag check is waived.",
  ["It is Auto Pass only","It is Auto Pass and CPV is waived","It is approved by Head of Credit","It is a Level 2 case"])
q("D","calc","Note C grid – Code 1","Employer remarks code <b>1</b>, Expat, scorecard <b>Medium</b> risk, salary AED 8,000. Maximum multiples-based amount in (a) STL (b) PDC?",
  C("(a) STL  → 20X × 8,000 = AED 160,000\n(b) PDC  → 10X × 8,000 = AED  80,000")+"Before DBR and other caps. If High risk → <b>Expat Auto Reject</b>.")
q("D","calc","Note C grid – Code 7","Employer code <b>7</b> (hourly-wage employer), Expat, Medium risk, STL, salary AED 6,000. Maximum? What if High risk?",
  C("Medium → 10X × 6,000 = AED 60,000")+"High risk → <b class='al'>Expat Auto Reject</b>. Locals → normal policy.")
q("D","calc","Note C grid – Code 6","Employer code <b>6</b>, <b>UAE National</b>, High risk, STL, salary AED 15,000. Maximum multiples-based amount and special requirement?",
  C("Code 6 applies to Locals &amp; Expats → High (STL) = 8X\n8 × 15,000 = AED 120,000")+"<b class='al'>HR CPV is mandatory</b> (fraud reasons).")
q("D","mcq","Note C grid – Code 2","Employer code <b>2</b>, Expat, <b>Very Low</b> risk. Decision?",
  "<b class='al'>C</b> – Expats are completely restricted for all risk profiles including Very Low and Low. Locals → normal policy.",
  ["Normal policy","20X allowed","Restricted","10X allowed"])
q("D","mcq","Note C grid – Code 8","Employer code <b>8</b> means:",
  "<b class='al'>A</b> – suspended employers, restricted for UAE Nationals and Expats, all risk profiles.",
  ["Suspended employer – restricted for everyone","Hourly-wage employer","Expats auto-reject only","HR CPV mandatory"])
q("D","case","Note D-I","Which of these is an AECB auto-reject? (a) Local, STL, score 300 (b) Expat, STL, score 610, overdue AED 11,000 (c) Expat, PDC, score 535 (d) Local, STL, score 600, overdue AED 9,000",
  "<b class='al'>(a), (b) and (c)</b>. (a) score 300 – all segments; (b) Expat overdue &gt; AED 10,000; (c) PDC with score &lt; 541. (d) is not an auto-reject (the &gt;10K overdue rule is for Expats).")
q("D","case","Note D-II","Current-overdue check (cards &gt; AED 1,000 / instalments &gt; AED 2,000), non-LS scheme. Is the check applied? (a) Expat, score 580, Medium risk (b) Local, score 580, Medium risk (c) Local, score 520, Low risk",
  "(a) <b class='al'>Yes</b> – Expat with score ≥ 541 and Medium/High risk.<br>(b) <b class='al'>No</b> – for Locals with score ≥ 541 only High risk is checked.<br>(c) <b class='al'>Yes</b> – score &lt; 541, all internal risk ratings.",
  unsure="The policy says these overdues are \"to be checked\" but does not state the outcome (reject or refer). Follow system/RCD instruction.")
q("D","case","Note D-II","Liability Settlement case: the facility being settled at Bank X is <b>35 DPD</b>; a Bank Y card that is NOT being settled has an overdue. What applies?",
  "Bank X: overdue up to <b>29 DPD</b> is allowed on facilities being settled → 35 DPD is <b class='al'>not acceptable</b>.<br>Bank Y: overdue must be <b>settled by the customer</b> and reflect as paid in AECB; AECB proof of clearance is mandatory.")
q("D","case","Note D-V","UAE National, salary transfer segment, Government Housing Program loan overdue at <b>75 DPD</b> (Bucket 3). Decision on that overdue?",
  "<b class='al'>Can be disregarded</b> – up to Bucket 3 (&lt; 90 DPD). Bucket 4+ must be settled and updated in AECB before approval. Applies only to Liability segment and salary transfer segment.")
q("D","case","Note D-I / D-IV","UAE National in PDC segment with AECB score 530. Auto-reject or referral?",
  "Rule I says Expats and UAE Nationals with score &lt; 541 in PDC → <b>Auto Reject</b>; Rule IV says Local under PDC with score &lt; 541 → <b>Referred</b>.",
  unsure="The two rules conflict for UAE Nationals. Confirm the system behaviour with RCPPM.")

# ---------------- PART E ----------------
q("E","calc","6.1","UAE National, age <b>19</b>, DIB salary transfer, salary AED 22,000. Maximum finance?",
  C("10 × 22,000 = 220,000   vs   250,000\nLower = AED 220,000")+"<b class='al'>AED 220,000</b> (STL relationship only – PDC not allowed; no court consent needed).")
q("E","calc","6.1","Same rule: UAE National aged 20, DIB salary AED 28,000. Maximum finance?",
  C("10 × 28,000 = 280,000   vs   250,000\nLower = AED 250,000")+"<b class='al'>AED 250,000</b>")
q("E","mcq","6.1","An Expat aged 19 asks for PF fully secured by his own Time Deposit. Is the age acceptable?",
  "<b class='al'>A</b> – cash-secured / against TD minimum age is 18 (otherwise 21).",
  ["Yes – minimum 18 for cash-secured","No – minimum 21 for everyone","Only with court consent","Only for UAE Nationals"])
q("E","calc","6.2 / 6.5","Expat, age <b>58</b>, salary AED 20,000 (non-MOD). Maximum tenure?",
  C("Max age at maturity (income &lt; 30K) = 60\n60 − 58 = 2 years = 24 months")+"<b class='al'>24 months</b>")
q("E","calc","6.2","Expat, age 59, salary AED 32,000, requests 48 months. Within policy? Any special handling?",
  C("Income ≥ 30K → max age 65\n59 + 4 years = 63 ≤ 65 ✔")+"<b class='al'>Within policy</b> – but age &gt; 60, so it is <b>referred at DDE</b> and STP at DDEQC.")
q("E","calc","6.2 / 6.5","UAE National, <b>pension only</b>, age 70. Maximum tenure? Can a second salary be added to income?",
  C("Max age (pension only) = 75 → 75 − 70 = 5 years = 60 months\nCBUAE max tenure (non-MOD) = 48 months\nTenure = lower = 48 months")+"<b class='al'>48 months</b>. Age &gt; 65 → no add-back of additional/second salary (a second pension can be added); referred at DDE.")
q("E","case","6.2","UAE National, age 80, PF against his own TD. Maximum tenure?",
  "<b class='al'>12 months</b> – Against TD, UAE National age &gt; 75 and &lt; 85 → 12 months (age &lt; 75 → 48 months).")
q("E","mcq","6.3","Which request is below the minimum finance amount?",
  "<b class='al'>B</b> – minimum is AED 10K for Fresh and AED 5K for Liability Consolidation (top-up).",
  ["Fresh AED 10,000","Fresh AED 9,000","Top-up AED 6,000","Top-up AED 5,000"],
  unsure="The policy does not say whether the 5K top-up minimum refers to the incremental amount or the new total; options C/D assume incremental.")
q("E","calc","6.4","Salary AED 24,000; existing monthly obligations (AECB) AED 5,500. Maximum new PF instalment?",
  C("50% × 24,000 = 12,000\n12,000 − 5,500 = AED 6,500")+"<b class='al'>AED 6,500 per month</b>")
q("E","calc","6.4","Pensioner, pension AED 15,000; existing obligations AED 1,000. Maximum new instalment?",
  C("30% × 15,000 = 4,500\n4,500 − 1,000 = AED 3,500")+"<b class='al'>AED 3,500 per month</b>")
q("E","calc","6.4","Dual income: pension AED 12,000 + salary AED 18,000. Calculate the maximum total obligations using (a) the 30%/50% method and (b) the Finnone 60%/100% method.",
  C("(a) 30% × 12,000 + 50% × 18,000 = 3,600 + 9,000 = 12,600\n(b) Income = 60% × 12,000 + 100% × 18,000 = 7,200 + 18,000 = 25,200\n    50% × 25,200 = 12,600")+"<b class='al'>Both = AED 12,600</b> – that is why the Finnone method never breaches DBR.")
q("E","mcq","6.4","For which case is DBR calculation NOT applicable?",
  "<b class='al'>C</b> – DBR is not applicable for finance against TD / cash secured. (DIB staff DBR is calculated by DIB-HR.)",
  ["DIB staff","Pensioners","Finance against TD / cash secured","PDC customers"])
q("E","calc","6.5 / 6.6","MOD employee, salary AED 18,000. Maximum multiples-based amount and maximum tenure?",
  C("15 × 18,000 = AED 270,000      Tenure up to 60 months")+"<b class='al'>AED 270,000 / 60 months</b>")
q("E","calc","6.4 / 6.6 / 7.1","UAE National, non-MOD, TML Cat C, STL, salary AED 15,000, no other debts, scorecard 20x. The system EMI for AED 300,000 over 48 months is AED 7,100. Final eligible amount?",
  C("Multiples cap : 20 × 15,000 = 300,000\nDBR cap       : 50% × 15,000 = 7,500 ≥ 7,100 ✔\nSegment cap   : Local STL Cat C = 5,000,000 (not binding)\nLowest        = AED 300,000")+"<b class='al'>AED 300,000</b> over 48 months.")
q("E","calc","6.7","Finance amount AED 250,000. Maximum value of the security cheque?",
  C("120% × 250,000 = AED 300,000")+"<b class='al'>AED 300,000</b> (Section 7.1: 120% of Net Finance Value; not required for cash-secured finance).")
q("E","mcq","6.5","A 3-month PF is requested. When is this possible?",
  "<b class='al'>D</b> – minimum tenure 6 months; less than 6 months allowed only for a finance secured by Investment.",
  ["Never","For MOD only","For UAE Nationals","When secured by an investment"])
q("E","calc","6.10","Customer applied for Home Finance on <b>10 January</b>. On <b>20 February</b> the customer applies for PF. Allowed? Earliest date?",
  "<b class='al'>Not allowed</b> – home finance applicants cannot get PF for 2 months (CBUAE Reg. 31/2013). Earliest = <b>10 March</b>.")
q("E","case","6.8","Which of these customers can get further financing? (a) AECB shows NDDS (b) NLS facility that will be closed by the new PF within CB tenure/multiple/DSR (c) Sheikh Zayed Housing facility of 360 months, never on NDDSF list (d) Bank's external auditor",
  "<b class='al'>(b) and (c)</b>. (a) NDDS → not allowed. (d) External auditors and their employees are restricted for retail products.")
q("E","mcq","6.8","Which structure is NOT permitted for PF?",
  "<b class='al'>A</b> – Personal finance should not be structured as an overdraft (OD).",
  ["Overdraft","Murabaha","Ijarah for services","Sukuk Murabaha (NBC)"])
q("E","mcq","6.9","The CPR affordability (lifestyle expenditure) assessment applies to:",
  "<b class='al'>C</b> – all salaried individuals and Self-Employed customers having a company with legal status as Sole Establishment.",
  ["Only Expats","Only PDC customers","All salaried + Self-Employed with Sole Establishment","Only digital journey"])

# ---------------- PART F ----------------
q("F","case","7.1 – Min income","Check minimum income: (a) Local, DIB, TML Cat B, AED 3,200 (b) Local, DIB, TML Cat D, AED 4,800 (c) Expat, PDC, Non-TML, AED 5,000",
  "(a) <b class='al'>Pass</b> (≥ 3,000)<br>(b) <b class='al'>Fail</b> (Cat D needs 5,000)<br>(c) <b class='al'>Pass</b> (PDC 5,000 for TML and Non-TML)")
q("F","calc","7.1 – Max finance","Expat, salary with another bank, TML Cat A, salary AED 40,000, Low risk. Maximum eligible amount (before DBR)?",
  C("Multiples: 20 × 40,000 = 800,000\nSegment cap Expat PDC (A–D) = AED 300,000\nLower = AED 300,000")+"<b class='al'>AED 300,000</b>")
q("F","case","7.1 – Max finance","State the maximum finance for: (a) Expat, DIB salaried, TML Cat D (b) Expat, DIB salaried, Non-TML, Medium risk (c) Local, PDC, Non-TML (d) Local, DIB salaried, Non-TML",
  "(a) <b class='al'>AED 1,000,000</b> (b) <b class='al'>AED 200,000</b> (c) <b class='al'>AED 200,000</b> (d) <b class='al'>AED 800,000</b>")
q("F","calc","7.1 – Reject multiples","Expat, <b>Medium</b> risk, salary AED 4,500 (DIB, TML Cat A). Customer requests AED 60,000. Decision?",
  C("Income &lt; 5K → max 12x → 12 × 4,500 = 54,000\nRequested 60,000 ÷ 4,500 = 13.3x &gt; 12x")+"<b class='al'>Auto-reject</b> (Note 2 – requested multiples more than specified).")
q("F","calc","7.1 – Reject multiples","Expat, Medium risk, salary AED 9,000. Maximum multiples-based amount if (a) DIB Salaried (b) PDC?",
  C("Band ≥8K – &lt;15K\n(a) DIB Salaried/LS 20x → 180,000\n(b) Others 15x       → 135,000")+"<b class='al'>(a) AED 180,000 (b) AED 135,000</b>")
q("F","case","7.1 – Reject multiples","(a) Expat, High risk, salary AED 20,000, valid AECB report received. (b) UAE National, High risk, STL. (c) UAE National, High risk, PDC. Scorecard decisions?",
  "(a) <b class='al'>0x – Hard Reject</b> (Note 1/5).<br>(b) <b class='al'>20x – Referral</b> (Note 3).<br>(c) <b class='al'>0x – Hard Reject</b>.")
q("F","mcq","7.1 – Note 6","Which Auto-pass case meeting all general-policy conditions and CPV waiver is NOT treated as STP?",
  "<b class='al'>B</b> – exceptions: 6.1 Expat, Medium, Income ≥ 8K and Non-TML; 6.2 UAE National High Risk.",
  ["Expat, Low risk, TML A","Expat, Medium risk, income AED 10,000, Non-TML","UAE National, Medium risk","Expat, Medium risk, income AED 7,000, TML B"])
q("F","case","7.1 – Contactless","What is the contactless per-customer cap for (a) Expat (b) UAE National?",
  "The policy states <b>AED 350</b> for Expat and <b>AED 750</b> for UAE Nationals; higher exposure is allowed under conventional contract types.",
  unsure="The figures are written as \"AED 350 / AED 750\". In the context of PF exposure they may mean AED 350K / 750K. Confirm with RCPPM.")
q("F","calc","7.1 – Contactless + Circular 15-Feb-2026","Contactless journey per-customer cap. Which requests can stay on the contactless journey? (a) Expat AED 300,000 (b) Expat AED 420,000 (c) UAE National AED 700,000 (d) UAE National AED 900,000",
  C("Caps (circular effective 15 Feb 2026): Expat 350K · UAE National 750K\n(a) 300,000 ≤ 350,000 ✔  (b) 420,000 &gt; 350,000 ✘\n(c) 700,000 ≤ 750,000 ✔  (d) 900,000 &gt; 750,000 ✘")+"<b class='al'>(a) and (c)</b>. (b) and (d) must be booked under conventional contract types. Since the circular, LS can also use the contactless journey (no signatures on any page) and TML Cat D is enabled in LS STL. Old caps were Local 500K / Expat 250K.")
q("F","case","7.1 – Payment history","Non-scored segment (not against TD). Last 12 months buckets: 0 0 2 2 0 0 0 0 0 0 0 0. Acceptable?",
  "<b class='al'>Yes</b> – same bucket repeated in 2 consecutive months is counted as 1; Bucket 2 allowed 1 time for other segments. Current bucket is 0.")
q("F","case","7.1 – Payment history","Non-scored segment. Buckets: 0 2 0 0 2 0 0 0 0 0 0 0. Acceptable (a) for a normal segment (b) for Against TD?",
  "Two separate Bucket-2 occurrences = 2 times.<br>(a) <b class='al'>No</b> (max 1) (b) <b class='al'>Yes</b> (max 2 for Against TD).")
q("F","case","7.1 – Rescheduled","Rescheduled accounts: (a) Expat rescheduled 4 months ago (b) Local rescheduled 4 months ago (c) Expat rescheduled 14 months ago. Decisions?",
  "(a) <b class='al'>Auto Reject</b> (0–6 months, Expat) (b) <b class='al'>Referred</b> (c) <b class='al'>Auto pass</b> (&gt; 12 months).")
q("F","case","7.1 – Payment holiday","Maximum days to first EMI: (a) UAE National, DIB salary AED 30,000 (b) Expat, PDC salary AED 8,000 (c) Expat, DIB salary AED 20,000 (d) DIB Staff",
  "(a) <b class='al'>240</b> (b) <b class='al'>90</b> (c) <b class='al'>150</b> (d) <b class='al'>120</b> days")
q("F","calc","7.1 – Payment holiday","Expat, PDC, salary AED 12,000. PF approved on <b>1 June</b>. Latest permitted first-EMI date?",
  C("PDC ≥10K &amp; &lt;35K → up to 120 days\nJune 1→30 = 29 days, July 31 → 60, August 31 → 91, September 29 → 120")+"<b class='al'>29 September</b>")
q("F","case","7.1 – EMI date","Choose the EMI date basis: (a) salaries on 25 Mar, 26 Apr, 24 May (b) 3 Mar, 14 Apr, 27 May.",
  "(a) Two repeated dates within a 3-day variance → <b class='al'>around the 25th</b>.<br>(b) All different → <b class='al'>last salary credit date (27th)</b>. The 1st or 2nd of any month can never be selected.")
q("F","case","7.1 – Returned cheques","Non-scored segment, last 3 months: (a) STL TML Cat B customer – 2 returned DDAs (insufficient funds) (b) PDC customer – 2 returned DDAs (different instruments). Within policy?",
  "(a) <b class='al'>Yes</b> – max 2 for TML A/B/C under STL (incl. LS) &amp; Against TD.<br>(b) <b class='al'>No</b> – max 1 for all others.")
q("F","case","7.1 – Returned cheques","One cheque bounced 3 times. How many counts? What if it was settled within 7 days with cash receipt showing amount and cheque number (original sighted)?",
  "<b class='al'>3 counts</b> (same cheque counted the times it bounced). If settled within 7 days with that proof → <b class='al'>not counted</b>.")
q("F","mcq","7.1 – DDEQC skip","Sales manually changed the employer name that Finnone auto-populated from Core. The case will be:",
  "<b class='al'>B</b> – Partial Credit Queue Skip (KYC, income or employer modified).",
  ["Complete DDEQC skip","Partial credit queue skip","Auto-rejected","Sent to SAM"])
q("F","mcq","7.1 – Payment holiday 2","In a DDEQC skip scenario, the default first EMI date is:",
  "<b class='al'>C</b> – 45 days from salary date; sales may modify it on customer confirmation (deviations referred to RCD as policy failure).",
  ["30 days from approval","90 days from salary date","45 days from salary date","Next salary date"])

# ---------------- PART G ----------------
q("G","calc","7.2","Salaried customer, annual income AED 55,000, blocks TD AED 300,000. Profit over the tenure = AED 9,000. Maximum principal?",
  C("Annual income ≤ 60K → 50%\nMax PF (principal + profit) = 50% × 300,000 = 150,000\nMax principal = 150,000 − 9,000 = AED 141,000")+"<b class='al'>AED 141,000</b>")
q("G","calc","7.2","Maximum PF (principal + profit): (a) Non-resident (tagged in core) blocking AED 500,000 (b) SIP-3D Maximizer AED 400,000 (c) Salaried, annual income AED 96,000, blocking AED 200,000",
  C("(a) 95% × 500,000 = 475,000\n(b) 90% × 400,000 = 360,000\n(c) &gt; 60K → 100% × 200,000 = 200,000")+"<b class='al'>(a) 475,000 (b) 360,000 (c) 200,000</b>")
q("G","case","7.2","Non-Wajaha/Ayaan customer, PF against TD: (a) maximum finance (b) maximum tenure with bullet payment (c) with monthly payment?",
  "(a) <b class='al'>AED 10 million</b> (Wajaha/Ayaan: 100 million) (b) <b class='al'>24 months</b> or up to SIP-3D settlement date (c) <b class='al'>48 months</b>")
q("G","calc","7.3","Auto Sukuk Pack, STL TML customer, salary AED 8,000, non-MOD, car price AED 180,000. Maximum Sukuk Pack (before DBR)?",
  C("30% × 180,000 = 54,000\nMultiples 20 × 8,000 = 160,000\nExposure cap 200,000\nLowest = AED 54,000")+"<b class='al'>AED 54,000</b>")
q("G","calc","7.3","PDC Non-TML customer, salary AED 12,000, buys a car of AED 260,000 from Gargash Motors. Maximum Sukuk Pack (before DBR)?",
  C("Dealer on approved list ✔   Min salary PDC Non-TML 10K ✔\n20% × 260,000 = 52,000\nNon-TML exposure cap = 50,000\nLower = AED 50,000")+"<b class='al'>AED 50,000</b>")
q("G","case","7.3","Who approves deviations in the Auto Sukuk Pack that are within CB regulations?",
  "<b class='al'>Head of Retail Credit.</b>")
q("G","case","7.4","HF Sukuk Pack: (a) Can a co-applicant get ancillary-cost finance? (b) Should PF Sales pull a new AECB? (c) Latest first EMI? (d) Do TML restrictions and CPV apply?",
  "(a) <b class='al'>No</b> – primary customers only (b) <b class='al'>No</b> – already pulled by HF team (c) <b class='al'>within 90 days</b>, matching salary date (d) <b class='al'>No</b> – HF eligibility parameters supersede.")
q("G","case","7.5","Cross-sell PF: which customers are excluded? (a) Pensioner (b) Joint account holder (c) Clearance letter issued 100 days ago (d) TML Cat A, DIB salary, age 30",
  "<b class='al'>(a), (b) and (c)</b> are excluded (pensioner; joint account holder; liability/clearance letter in last 120 days). (d) is acceptable.")
q("G","calc","7.5","Cross sell, UAE National, income AED 30,000. Total outstanding with DIB AED 100,000; primary AF AED 250,000 (car value AED 300,000); existing covered card limit AED 30,000; no secondary card. Requested PF AED 280,000. Check aggregate exposure and asset value.",
  C("Aggregate = 100,000 + 250,000 + 30,000 + 280,000 + 0 = 660,000 ≤ 1,000,000 ✔\nPF 280,000 &lt; asset value 300,000 ✔ ; ≤ 1,000,000 national cap ✔\nMultiples 280,000 ÷ 30,000 = 9.3x ≤ 20x ✔")+"<b class='al'>Passes</b> these checks (DBR still to be checked).")
q("G","calc","7.5","Same customer requests PF AED 320,000. Allowed?",
  "<b class='al'>No</b> – PF exposure must be less than the primary product's asset value (AED 300,000).")
q("G","calc","7.5","Cross sell: Expat, income AED 12,000, non-MOD, already has a PF of AED 150,000 (original amount). Maximum new cross-sell PF under the multiples rule?",
  C("Aggregate multiples (existing + new) ≤ 20x\n20 × 12,000 = 240,000 − 150,000 = AED 90,000")+"<b class='al'>AED 90,000</b> (subject to min AED 10,000, DBR, exposure caps).",
  unsure="The policy does not state whether the existing PF is counted at its original amount or its current outstanding when computing aggregate multiples. This answer uses the amount given.")
q("G","case","7.5","Cross sell: (a) minimum assessed income (b) approval validity (c) aggregate exposure limit for Expats (d) what happens to the RCD and CPV stages?",
  "(a) <b class='al'>AED 8,000</b> (b) <b class='al'>up to 30 days</b> from primary product last credit approval (c) <b class='al'>AED 500,000</b> (d) <b class='al'>bypassed</b> – goes directly to PDOC for affordability.")
q("G","calc","7.6.1","Wajaha client: dividends (latest year credit) AED 240,000; rental income (EJARI) AED 180,000 per year. Monthly assessed income and finance eligibility?",
  C("Dividends 100% → 240,000 ÷ 12 = 20,000\nRent 83% → 180,000 × 83% = 149,400 ÷ 12 = 12,450\nTotal = 32,450 per month\n20x = AED 649,000   (Wajaha max 1,000,000)")+"<b class='al'>AED 649,000</b> (subject to DBR).")
q("G","calc","7.6.1","Bonus received in the last 3 years: AED 90,000, 70,000, 100,000. Monthly income from bonus?",
  C("Minimum of 3 years = 70,000 × 50% = 35,000 ÷ 12 = AED 2,916.67")+"<b class='al'>≈ AED 2,917 per month</b>")
q("G","case","7.6","(a) Max finance for Ayaan (b) minimum Ayaan relationship (c) is a lien on deposit mandatory (d) can an Ayaan customer categorised on salary use the 7.6.1 add-backs?",
  "(a) <b class='al'>AED 500K</b> (Wajaha AED 1M) (b) <b class='al'>12 months</b> (Wajaha nil) (c) <b class='al'>No</b> – non-mandatory (d) <b class='al'>No</b> – only Ayaan categorised on wealth with DIB.")

# ---------------- PART H ----------------
q("H","case","7.7.1 / 7.7.7","Relief eligibility: (a) customer at 92 DPD (b) last salary credited 45 days ago (c) total DIB liability AED 9,000 (d) income unchanged, DBR 55% due to new outside loans",
  "(a) <b class='al'>Refer to SAM</b> (Bucket 4+) (b) <b class='al'>Not yet</b> – one salary must be credited within the last 40 days (c) <b class='al'>Not eligible</b> – minimum liability AED 10,000 (d) <b class='al'>Not eligible</b> – stressed with same income level.")
q("H","calc","7.7.10","Relief: salary cut to AED 12,000; outside debt EMI AED 800. Maximum new Relief EMI?",
  C("50% × 12,000 = 6,000\n6,000 − 800 = AED 5,200")+"<b class='al'>AED 5,200</b> (tenure extended – no cap – until EMI fits).")
q("H","calc","7.7.10","Relief: customer changed status to pensioner, pension AED 10,000, outside EMI AED 500. Maximum Relief EMI?",
  C("30% × 10,000 = 3,000\n3,000 − 500 = AED 2,500")+"<b class='al'>AED 2,500</b>")
q("H","calc","7.7.7.6 / 7.7.7.9","Relief: Expat aged 50. Maximum tenure possible? UAE National aged 50?",
  C("Expat: max age at maturity 65 → 65 − 50 = 15 years = 180 months")+"Expat <b class='al'>180 months</b>. UAE National: <b class='al'>no age cap</b> and tenure has no cap – aligned to bring DBR within CBUAE limits.")
q("H","calc","7.7.5.1","Before relief: salary dropped from AED 20,000 to AED 14,000. DIB EMIs AED 9,000 + outside EMI AED 1,000. Current DBR? Does the customer qualify on DBR?",
  C("(9,000 + 1,000) ÷ 14,000 = 71.4%  &gt; 50%")+"<b class='al'>Yes</b> – DBR above CBUAE limit for a non-pensioner.")
q("H","case","7.7.7.11 / 7.7.7.12 / 7.7.7.5","(a) When is the first Relief instalment collected? (b) After how many regular EMIs can a postponement be allowed? (c) MOB setting?",
  "(a) <b class='al'>Next salary date (within 30 days)</b>; up to 90 days with HORC approval (b) <b class='al'>6 regular EMIs</b> (c) <b class='al'>3 months</b> regardless of paid instalments; 1 month with HORC approval.")
q("H","case","7.7.11","After Relief: (a) 7 regular payments, income unchanged, wants new PF (b) 4 regular payments, moved to a better-paid employer",
  "(a) <b class='al'>No</b> – needs 12 regular payments (b) <b class='al'>Can be considered</b> after 3 instalments, but the Relief contract must be settled by the customer and the new finance must follow normal policy and CBUAE norms.")
q("H","calc","7.7.15.7","Relief security cheque: (a) Ijara finance amount AED 180,000 (b) Murabaha principal AED 200,000 + profit AED 30,000",
  C("(a) 100% × 180,000 = AED 180,000\n(b) Debit balance = 200,000 + 30,000 = AED 230,000")+"<b class='al'>(a) AED 180,000 (b) AED 230,000</b>")
q("H","case","7.7.8.2","Customer seeking Relief has a TD-secured PF and an unsecured PF. What must happen first?",
  "The cash-secured facility must be <b class='al'>closed</b> and residual collateral used to pay the other facility. If DBR is still above the CBUAE threshold, Relief can be accepted. Relief without liquidating the collateral needs <b>HORC</b> approval.")
q("H","case","7.7.14 / 7.7.7.23 / 7.7.13","After Relief approval: how is the account reported in AECB, which ECL stage, what happens in core banking, and what fees apply?",
  "<b class='al'>\"Arrangement\"</b> in AECB; <b class='al'>Stage 2</b> for ECL; customer <b>blacklisted at CIF level</b> in core banking; <b>no fees or charges</b> for due diligence and documentation.")
q("H","calc","8.4","Relief settlement: PF outstanding AED 150,000 (incl. unrealised profit 12,000), AF AED 70,000 (incl. unrealised profit 4,000), covered card AED 18,000. Amount of the Relief facility needed? Is it less than combined outstanding?",
  C("PF  150,000 − 12,000 = 138,000  (rebate)\nAF   70,000 −  4,000 =  66,000  (rebate)\nCard 18,000 (no rebate on cards)\nRelief facility = 138,000 + 66,000 + 18,000 = AED 222,000\nCombined outstanding = 238,000 → 222,000 &lt; 238,000 ✔")+"<b class='al'>AED 222,000</b> – no additional liquidity (AAOIFI SS 59, cl. 4.1.3.3). A DIB Home Finance would <b>not</b> be settled from these proceeds (profit rate may be reduced or asset sold at a discount).",
  unsure="Rebate figures are given in the question; in practice actual rebates are calculated by the system/Operations per contract.")
q("H","mcq","8.4","Under the Relief settlement process, what happens to an existing DIB Home Finance?",
  "<b class='al'>C</b> – HF is not settled by Commodity Murabaha proceeds; the Bank may reduce future profit rates or sell the asset at a discounted price.",
  ["It is settled first","It is merged into the Relief facility","It is not settled with the proceeds; profit rate may be reduced or asset sold at a discount","It is transferred to SAM"])

# ---------------- PART I ----------------
q("I","case","8.1 – Passport","Is the visa page required? (a) Expat, latest EID issued 15 May 2022 with employer name (b) Expat, EID issued January 2022 (c) Saudi national",
  "(a) <b class='al'>No</b> – EID issued after 10 April 2022 with employer name (b) <b class='al'>Yes</b> (c) <b class='al'>No</b> – GCC nationals exempt.")
q("I","case","8.1 – UAE ID","Customer's Emirates ID is under renewal. The EID application form is 23 days old. Acceptable with the passport (original seen)?",
  "<b class='al'>No</b> – the application should not be more than 21 days old.")
q("I","mcq","8.1 – AECB consent","Who validates the customer's AECB consent?",
  "<b class='al'>B</b> – sales supervisor under maker-checker; no validation by the RCD analyst.",
  ["RCD analyst","Sales supervisor","Compliance","Operations (RAO)"])
q("I","case","8.1 – Govt income docs","Fresh case, Government employee. Required income document if: (a) no salary in DIB (b) 2 salaries in DIB (c) 3 regular salaries in DIB",
  "(a) <b class='al'>SC addressed to DIB with IBAN/full A/C</b> (salary count = 1) (b) <b class='al'>SC to \"To Whom It May Concern\" or DIB</b> (c) <b class='al'>Employee ID only</b> (SC if salary irregular).")
q("I","calc","8.1 – Validity","Private TML employer STL dated <b>3 April</b>, submitted on <b>5 May</b>. Valid?",
  C("3 Apr → 30 Apr = 27 days + 5 May = 32 days &gt; 30 days")+"<b class='al'>Expired</b> – income documents from TML private employers are valid 30 days at submission.")
q("I","case","8.1 – SC","SC from a Non-TML company signed by the owner, who is the customer's brother. Acceptable?",
  "<b class='al'>No</b> – SC signed by the owner of a Non-TML employer is not acceptable if he/she is the spouse/sibling/parent/child of the customer.")
q("I","case","8.1 – Cross sell SC","SC validity for cross sell: (a) Salaried/Privileged (b) PDC",
  "(a) <b class='al'>60 days</b> from issuance (or as per SC validity) (b) <b class='al'>30 days</b>. If elapsed → fresh SC.")
q("I","calc","8.1 – Salary variance","Last 3 salaries: AED 12,000 / 15,000 / 12,000. (a) Is variance within policy? (b) For a DDEQC-skip deal, which salary is used?",
  C("(15,000 − 12,000) ÷ 12,000 = 25%  &gt; 20%")+"(a) <b class='al'>No</b> – documentary proof (pay slip, company letter) may be required; CPV confirmation acceptable if due to performance/regular bonus. (b) <b class='al'>Lowest of last 3 = AED 12,000</b>.")
q("I","case","8.1 – Irregular salary","Salary dates, 'Others' segment (not STL/LS): 3rd, 5th, 13th. Regular?",
  C("3rd &amp; 5th within ±3 days ✔ ; 13th is 8 days from the 5th &gt; ±7 days ✘")+"<b class='al'>Irregular.</b> (For STL/LS the rule is ±5 days for two salaries and the third is not checked.)")
q("I","case","8.1 – EMI date","(a) DIB salaried, salary credited on the 20th – latest EMI date? (b) PDC, salary on the 15th – EMI date?",
  "(a) <b class='al'>Up to the 25th</b> (up to 5 days) (b) <b class='al'>16th</b> (+1 day). EMI never on the 1st or 2nd.")
q("I","case","8.1 – Liability letter","(a) Liability letter addressed to DIWAN (b) LL from retiring bank's Collections unit; customer paid regularly for the last 4 months. Acceptable?",
  "(a) <b class='al'>Not acceptable</b> (may only be approved as a deviation by HOI and above – Annex 8.2) (b) <b class='al'>Not acceptable</b> – needs regular payments for the past 6 months.")
q("I","case","8.1 – CC settlement","Card-only external liability: limit AED 50,000, statement outstanding AED 32,000. Which figure is used for DSR and which for settlement?",
  "<b class='al'>DSR on the card limit (AED 50,000); settlement on the outstanding (AED 32,000).</b> Clearance/NOC letter required; no mandatory check on card utilisation.")
q("I","case","8.1 – Joint / Long tenure","(a) Joint PF with a cousin, STL TML (b) Expat earning AED 9,500 with an existing facility of remaining tenure 56 months wants new PF to settle it (c) a driver on an individual (domestic worker) visa (d) outsourced staff working at a bank",
  "(a) <b class='al'>No</b> – only husband/wife, parents, brothers/sisters, son/daughter (b) <b class='al'>No</b> – Expats need income ≥ AED 10,000 and no deviation is allowed on these parameters. (c) <b class='al'>Restricted</b> – domestic workers under individual visa (d) <b class='al'>Restricted</b> – financial-institution outsource staff are restricted in all segments.")
q("I","mcq","8.1 – Restrictions","Which applicant is restricted?",
  "<b class='al'>D</b> – domestic workers under individual visa (cooks, drivers, etc.) are restricted (also Bedouin/Stateless, with Marsoom, FI outsource staff, Non-TML outsource).",
  ["A lawyer","A judge","A diplomat","A driver on an individual (domestic) visa"])

# ---------------- PART J ----------------
q("J","calc","8.2 – Age","Expat, salary &lt; 30K (policy max age 60). Age at maturity: (a) 62 (b) 64 (c) 66. Deviation level?",
  C("(a) 2 yrs over  → Level 2\n(b) 4 yrs over  → Level 3 (&gt;2, ≤5)\n(c) 6 yrs over  → beyond Level 3")+"<b class='al'>(a) Level 2 (b) Level 3 (c) Cannot be approved</b>")
q("J","calc","8.2 – Income","TML Cat A (policy minimum AED 3,000). Salary: (a) AED 2,880 (b) AED 2,800 (c) AED 2,650. Level?",
  C("(a) 120 ÷ 3,000 = 4%     → Level 2 (≤5%)\n(b) 200 ÷ 3,000 = 6.67%  → Level 3 (≤10%)\n(c) 350 ÷ 3,000 = 11.67% → beyond Level 3")+"<b class='al'>(a) Level 2 (b) Level 3 (c) Not approvable</b>")
q("J","calc","8.2 – Multiples","Expat Medium risk, salary AED 7,000 (policy 14x). Customer needs (a) 16x (b) 17x. Level?",
  C("(a) +2 → Level 2   (b) +3 → Level 3 (≤ +4)   both ≤ 20x CB limit")+"<b class='al'>(a) Level 2 (b) Level 3</b>")
q("J","case","8.2 – Misc","Level required: (a) only 2 salary credits (policy 3) (b) only 1 salary credit (c) current overdue AED 650 at application (d) residence visa expired 10 days ago (renewal under process) (e) discount of 30% on profit for cash-secured finance",
  "(a) <b class='al'>Level 2</b> (1 salary short) (b) <b class='al'>Level 3</b> (2 short) (c) <b class='al'>Not approvable</b> (Level 3 max AED 500) (d) <b class='al'>Level 3</b> (&gt;3 days, ≤15) (e) <b class='al'>Level 3</b> (&gt;25%, ≤50%)")
q("J","case","8.2 – Generic","(a) Up to what amount can the CCO approve credit/documentation deviations for PF? (b) Who can waive an internal negative check on an overall basis due to system failure?",
  "(a) <b class='al'>AED 4 million</b> (with justification and business-head recommendation) (b) <b class='al'>Head of Retail Credit only</b> (deputies only if HORC is on leave).")
q("J","calc","8.3","Self-employed (under deviation), UAE National, Dubai mainland, established income AED 50,000 per month. Maximum finance?",
  C("Local 4.5x × 50,000 = 225,000\nCap UAE National (excl. Non-Dubai FZ) = 350,000\nLower = AED 225,000")+"<b class='al'>AED 225,000</b> (field visit mandatory).")
q("J","calc","8.3","Self-employed (under deviation): company average monthly credit turnover AED 200,000; applicant's share in trade licence 40%. Turnover attributable to the applicant? Is the minimum number of employees waived?",
  C("200,000 × 40% = AED 80,000")+"<b class='al'>AED 80,000</b>. Minimum employees <b>waived</b> because average monthly credit turnover &gt; 50K.")
q("J","calc","8.3","Mumayyaz self-employed customer: deposit relationship per PFS report AED 2,000,000. Maximum finance?",
  C("15% × 2,000,000 = 300,000   vs   350,000 → lower = AED 300,000")+"<b class='al'>AED 300,000</b>")

REMOVE = [
 "An application fails one policy-engine parameter", "Sharia principles for this policy", "In this policy \"PDC\" refers",
 "PF against an investment deposit: which deposit currency", "A customer wants PF against an FD that belongs",
 "Which statement about the Sukuk Pack segment", "An Ayaan Exclusive Banking customer with 14 months",
 "Which products can be consolidated", "Eligible BFIs are assigned", "Outsourced staff of a bank apply",
 "A case is Straight Pass (STP) when", "Employer remarks code <b>8</b>", "Employer code <b>2</b>, Expat",
 "A 3-month PF is requested", "Which structure is NOT permitted", "The CPR affordability",
 "For which case is DBR calculation NOT applicable", "An Expat aged 19 asks for PF fully secured",
 "Sales manually changed the employer name", "In a DDEQC skip scenario, the default", "What is the contactless per-customer cap",
 "Non-scored segment. Buckets: 0 2 0 0 2", "One cheque bounced 3 times", "Choose the EMI date basis",
 "Who approves deviations in the Auto Sukuk Pack", "Under the Relief settlement process, what happens to an existing DIB Home",
 "Who validates the customer's AECB consent", "Which applicant is restricted", "SC validity for cross sell",
]
Q = [x for x in Q if not any(x[3].startswith(r) for r in REMOVE)]
assert len(Q) == 100, len(Q)

def esc_opts(opts):
    return "<ol class='opt'>" + "".join(f"<li>{o}</li>" for o in opts) + "</ol>" if opts else ""

TYPE_LBL = {"calc": ("CALCULATION","t-calc"), "mcq": ("MULTIPLE CHOICE","t-mcq"), "case": ("CASE / SHORT ANSWER","t-case")}

body = []
body.append("""<div class="cover" style="background:linear-gradient(150deg,#6b3fa0 0%,#1f5fbf 50%,#0e7c86 100%)">
<div><span class="tag">EXAM PRACTICE • INTERNAL USE</span>
<h1>Personal Finance<br>Credit Policy<br>100 Exam Questions</h1>
<h2>Calculation-focused practice exam with full answers and workings under every question</h2></div>
<div class="meta"><b>Based on:</b> DIB Personal Finance Operating Credit Policy – ref. 2026-07-0005 | 002306 (22 pages)<br>
<b>Coverage:</b> every section – Definitions, Segments, LC/LS/BFI, Scorecard &amp; AECB, Minimum Financing Standards, Target Markets 7.1–7.7, Annexures 8.1–8.4<br>
<b>Accuracy:</b> answers follow the policy text. Where the policy is unclear or inconsistent, the answer is marked with a yellow <b>NOT 100% SURE</b> box.</div></div>
<div class="page-break"></div>
<div class="banner b-purple"><div class="num">HOW TO USE</div><h2>Instructions &amp; coverage map</h2></div>
<p class="legend"><span class="tag t-calc" style="color:#fff;padding:1px 7px;border-radius:9px;font-size:8pt;font-weight:bold">CALCULATION</span>
<span class="tag t-mcq" style="color:#fff;padding:1px 7px;border-radius:9px;font-size:8pt;font-weight:bold">MULTIPLE CHOICE</span>
<span class="tag t-case" style="color:#fff;padding:1px 7px;border-radius:9px;font-size:8pt;font-weight:bold">CASE / SHORT ANSWER</span></p>
<ul><li>Cover the green answer box, attempt the question, then check the answer and working.</li>
<li>All amounts are AED. All customers and figures are illustrative; the rules come from the policy.</li>
<li>"Before DBR" means the amount still has to pass the DBR / affordability check.</li>
<li><span style="background:#fff6c2;padding:1px 5px;border-left:4px solid #e0a800">Yellow box</span> = the policy wording is unclear or conflicting – confirm with RCPPM.</li></ul>
""")
# coverage table
counts = {}
for p,t,*_ in Q:
    counts.setdefault(p,{"calc":0,"mcq":0,"case":0}); counts[p][t]+=1
rows = "".join(f"<tr><td><b>{SECTIONS[p][1]}</b></td><td>{SECTIONS[p][2]}</td><td class='c'>{c['calc']}</td><td class='c'>{c['mcq']}</td><td class='c'>{c['case']}</td><td class='c'><b>{sum(c.values())}</b></td></tr>" for p,c in counts.items())
tot = {k:sum(c[k] for c in counts.values()) for k in ("calc","mcq","case")}
body.append(f"<table class='t-purple'><thead><tr><th>Part</th><th>Policy area</th><th class='c'>Calc</th><th class='c'>MCQ</th><th class='c'>Case</th><th class='c'>Total</th></tr></thead><tbody>{rows}<tr><td colspan='2'><b>Total</b></td><td class='c'><b>{tot['calc']}</b></td><td class='c'><b>{tot['mcq']}</b></td><td class='c'><b>{tot['case']}</b></td><td class='c'><b>100</b></td></tr></tbody></table>")
unsure_list = [i+1 for i,x in enumerate(Q) if x[6]]
body.append(f"<div class='box warn'><span class='lbl'>QUESTIONS WITH A 'NOT 100% SURE' NOTE</span><br>Q{', Q'.join(map(str,unsure_list))} – the answer follows the policy text, but the policy itself is unclear on the highlighted point.</div>")

cur = None
for i,(p,t,ref,text,opts,ans,uns) in enumerate(Q,1):
    if p != cur:
        cur = p; cls,num,title = SECTIONS[p]
        body.append(f'<div class="page-break"></div><div class="banner {cls}"><div class="num">{num}</div><h2>{title}</h2></div>')
    lbl,tc = TYPE_LBL[t]
    u = f'<div class="unsure"><b>⚠ NOT 100% SURE:</b> {uns}</div>' if uns else ""
    body.append(f'<div class="qq"><div class="qh"><span class="qn">Q{i}</span><span class="tag {tc}">{lbl}</span><span class="ref">Policy: {ref}</span></div>'
                f'<div>{text}</div>{esc_opts(opts)}<div class="aa"><b>Answer:</b> {ans}</div>{u}</div>')

out = HEAD.replace("</head>", EXTRA_CSS + "</head>") if "</head>" in HEAD else HEAD + EXTRA_CSS
(HERE/"src").mkdir(exist_ok=True)
(HERE/"src/00_head.html").write_text(out)
(HERE/"src/01_body.html").write_text("\n".join(body) + "\n</body></html>")
print("questions:", len(Q), "unsure:", unsure_list, tot)
