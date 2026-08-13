# Project Context

The **domain glossary** for this project — the ubiquitous language. It gives names
to the concepts the code is about, so that class names, method names, test names,
and conversation all use the same words for the same things.

This is a **starting point**. Replace the example terms below with this project's
actual domain language and grow it as the model sharpens. Several skills read this
file (`tdd`, `diagnose`, `improve-codebase-architecture`, `review`) to align their
vocabulary with yours.

> **How to maintain it**
> - **Be opinionated.** When several words mean the same thing, pick one and list
>   the rest under `_Avoid_`.
> - **Keep definitions tight** — one or two sentences. Define what a term *is*,
>   not what it *does*.
> - **Only domain terms.** General programming concepts (timeouts, DTOs, repos)
>   and framework terms (Vaadin `Grid`, Spring `@Service`) do **not** belong here,
>   even if used heavily. Before adding a term ask: *is this unique to our domain,
>   or a general/technical concept?* Only the former belongs.
> - **Group under subheadings** when natural clusters emerge; a flat list is fine
>   if all terms belong to one cohesive area.

---

## Language

> ⚠️ The entries below are **examples** showing the format. Delete them and add
> your own domain terms.

**Order**:
A customer's confirmed request for one or more products at agreed prices.
_Avoid_: Purchase, transaction

**Customer**:
A person or organisation that places orders.
_Avoid_: Client, buyer, account

**Invoice**:
A request for payment sent to a customer after fulfilment.
_Avoid_: Bill, payment request

---

## Multiple contexts

Most repos need only this single root `CONTEXT.md`. If the project grows into
several bounded contexts, replace this section with a top-level **`CONTEXT-MAP.md`**
that lists each context, where it lives, and how they relate — and give each
context its own `CONTEXT.md`. Example map:

```md
# Context Map

## Contexts

- [Ordering](./src/ordering/CONTEXT.md) — receives and tracks customer orders
- [Billing](./src/billing/CONTEXT.md) — generates invoices and processes payments

## Relationships

- **Ordering → Billing**: Ordering emits `OrderPlaced` events; Billing consumes
  them to generate invoices
- **Ordering ↔ Billing**: shared types for `CustomerId` and `Money`
```

## Related records

Decisions about *how* the system is built (architectural shape, integration
patterns, deliberate deviations) are not glossary terms — record them as ADRs
under `docs/adr/` (`0001-slug.md`, one short paragraph each). Cite them from
`STYLEGUIDE.md` where they constrain how code is written.
