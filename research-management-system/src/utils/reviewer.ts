import type { KeycloakUser } from '@/api/user'
import { UserRole } from '@/types'

export interface ReviewerOption {
  id: number
  label: string
  reviewerName: string
}

export function buildReviewerOptions(...reviewerGroups: Array<KeycloakUser[] | undefined>): ReviewerOption[] {
  const mergedReviewers = new Map<number, KeycloakUser>()

  reviewerGroups
    .flatMap((group) => group ?? [])
    .forEach((reviewer) => {
      const id = Number(reviewer?.id)
      if (!Number.isFinite(id)) return

      const existing = mergedReviewers.get(id)
      if (!existing) {
        mergedReviewers.set(id, {
          ...reviewer,
          id,
          roles: mergeRoles(reviewer?.roles),
          enabled: Boolean(reviewer?.enabled)
        })
        return
      }

      existing.roles = mergeRoles(existing.roles, reviewer?.roles)
      existing.name = existing.name || reviewer?.name || ''
      existing.username = existing.username || reviewer?.username || ''
      existing.email = existing.email || reviewer?.email || ''
      existing.enabled = existing.enabled || Boolean(reviewer?.enabled)
    })

  return Array.from(mergedReviewers.values())
    .sort((left, right) => getReviewerName(left).localeCompare(getReviewerName(right)))
    .map((reviewer) => {
      const reviewerName = getReviewerName(reviewer)
      return {
        id: Number(reviewer.id),
        reviewerName,
        label: `${reviewerName}${getReviewerRoleSuffix(reviewer)}`
      }
    })
}

function getReviewerRoleSuffix(reviewer: Partial<KeycloakUser>) {
  const roles = mergeRoles(reviewer?.roles)
  const labels: string[] = []

  if (roles.includes(UserRole.ADMIN)) {
    labels.push('管理员')
  }
  if (roles.includes(UserRole.EXPERT)) {
    labels.push('专家')
  }

  return labels.length ? `（${labels.join('/')}）` : ''
}

function mergeRoles(...roleGroups: Array<string[] | undefined>) {
  return Array.from(new Set(roleGroups.flatMap((roles) => roles ?? [])))
}

function getReviewerName(reviewer: Partial<KeycloakUser>) {
  const name = reviewer?.name?.trim()
  if (name) return name

  const username = reviewer?.username?.trim()
  if (username) return username

  const email = reviewer?.email?.trim()
  if (email) return email

  return `审核人${reviewer?.id ?? ''}`
}
