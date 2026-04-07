import { Card, CardContent } from '@/components/ui/card'

export function MiniStat({
  label,
  value,
  detail,
}: {
  label: string
  value: string
  detail: string
}) {
  return (
    <Card className="border-border/70 bg-background/70 shadow-sm">
      <CardContent className="p-4">
        <div className="text-[11px] uppercase tracking-[0.22em] text-muted-foreground">{label}</div>
        <div className="mt-2 text-xl font-semibold tracking-tight">{value}</div>
        <div className="mt-1 text-xs leading-5 text-muted-foreground">{detail}</div>
      </CardContent>
    </Card>
  )
}
