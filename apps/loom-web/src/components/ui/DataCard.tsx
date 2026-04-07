import { cn } from '@/lib/utils'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge, badgeVariants } from '@/components/ui/badge'
import { Progress } from '@/components/ui/progress'
import { Skeleton } from '@/components/ui/skeleton'
import type { LucideIcon } from 'lucide-react'
import type { VariantProps } from 'class-variance-authority'

type BadgeVariant = VariantProps<typeof badgeVariants>['variant']

interface DataCardProps {
  title?: string
  icon?: LucideIcon
  iconClassName?: string
  badge?: { label: string; variant?: BadgeVariant }
  progress?: { value: number; label?: string }
  loading?: boolean
  action?: React.ReactNode
  variant?: 'default' | 'stat'
  children?: React.ReactNode
  className?: string
}

export function DataCard({
  title,
  icon: Icon,
  iconClassName,
  badge,
  progress,
  loading = false,
  action,
  variant = 'default',
  children,
  className,
}: DataCardProps) {
  if (loading) {
    return (
      <Card className={className}>
        <CardHeader className="pb-2">
          <Skeleton className="h-5 w-32" />
        </CardHeader>
        <CardContent>
          <Skeleton className="h-20" />
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className={cn(variant === 'stat' && 'text-center', className)}>
      {(title || badge || action || Icon) && (
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <div className="flex items-center gap-2">
            {Icon && (
              <Icon className={cn('h-5 w-5 text-primary', iconClassName)} />
            )}
            {title && (
              <CardTitle className="text-base font-medium">
                {title}
              </CardTitle>
            )}
          </div>
          <div className="flex items-center gap-2">
            {badge && (
              <Badge variant={badge.variant} className="text-xs">
                {badge.label}
              </Badge>
            )}
            {action}
          </div>
        </CardHeader>
      )}
      <CardContent className={cn(variant === 'stat' && 'pt-0')}>
        {children}
        {progress && (
          <div className="mt-4 space-y-1">
            {progress.label && (
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">{progress.label}</span>
                <span className="font-medium">{progress.value}%</span>
              </div>
            )}
            <Progress value={progress.value} className="h-2" />
          </div>
        )}
      </CardContent>
    </Card>
  )
}

interface StatCardProps extends Omit<DataCardProps, 'variant' | 'title'> {
  title?: string
}

export function StatCard({
  title,
  ...props
}: StatCardProps) {
  return <DataCard variant="stat" title={title} {...props} />
}
