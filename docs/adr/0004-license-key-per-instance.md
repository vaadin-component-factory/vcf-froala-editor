# The license key is set per instance, with no global default

The component exposes a plain `setLicenseKey(String)` on each editor, and nothing else:
no static setter, no application-wide default, no configuration lookup. Storing a key
globally is not this add-on's job, and a Spring-bound key cannot live in the component
anyway (ADR-0003). Consumers normally hold one key and pass it at construction, which
is what comparable add-ons do; the Spring binding belongs in the application, or later
in a separate optional module.
