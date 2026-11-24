# Minification is disabled for this project, but if it is ever enabled these rules keep
# the Gson DTOs intact (Gson reads field names reflectively).
-keep class com.example.recipefinder.data.remote.dto.** { *; }
-keepattributes Signature, *Annotation*
