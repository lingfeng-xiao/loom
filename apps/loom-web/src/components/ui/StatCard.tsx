import * as React from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'
import { LucideIcon, TrendingDown, TrendingUp, Minus } from 'lucide-react'

const statCardVariants = cva(
  'rounded-xl border bg-background/70 p-4 shadow-sm transition-colors',
  {
    variants: {
      variant: {
        default: '',
        highlight: 'border-primary/30 bg-primary/5',
        muted: 'bg-muted/30',
      },
      size: {
        sm: 'p-3',
        md: 'p-4',
        lg: 'p-6',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'md',
    },
  }
)

export interface StatCardProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof statCardVariants> {
  label: string
  value: string | number
  icon?: LucideIcon
  trend?: {
    direction: 'up' | 'down' | 'neutral'
    value: string
  }
}

function StatCard({
  className,
  label,
  value,
  icon: Icon,
  trend,
  variant,
  size,
  ...props
}: StatCardProps) {
  const TrendIcon =
    trend?.direction === 'up'
      ? TrendingUp
      : trend?.direction === 'down'
        ? TrendingDown
        : Minus

  const trendColor =
    trend?.direction === 'up'
      ? 'text-green-600 dark:text-green-400'
      : trend?.direction === 'down'
        ? 'text-red-600 dark:text-red-400'
        : 'text-muted-foreground'

  return (
    <div className={cn(statCardVariants({ variant, size }), className)} {...props}>
      <div className="flex items-start justify-between gap-2">
        <div className="space-y-1">
          <div className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
            {label}
          </div>
          <div className="text-2xl font-semibold tracking-tight tabular-nums">
            {value}
          </div>
          {trend && (
            <div className={cn('flex items-center gap-1 text-xs', trendColor)}>
              <TrendIcon className="h-3 w-3" />
              <span>{trend.value}</span>
            </div>
          )}
        </div>
        {Icon && (
          <div className="rounded-lg bg-primary/10 p-2 text-primary">
            <Icon className="h-4 w-4" />
          </div>
        )}
      </div>
    </div>
  )
}

export { StatCard, statCardVariants }
