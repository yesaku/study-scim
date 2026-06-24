{{- define "study-scim.name" -}}
{{- .Chart.Name }}
{{- end }}

{{- define "study-scim.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "study-scim.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/name: {{ include "study-scim.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "study-scim.selectorLabels" -}}
app.kubernetes.io/name: {{ include "study-scim.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "study-scim.postgresql.host" -}}
{{- printf "%s-postgresql" .Release.Name }}
{{- end }}

{{- define "study-scim.valkey.host" -}}
{{- printf "%s-valkey-master" .Release.Name }}
{{- end }}
