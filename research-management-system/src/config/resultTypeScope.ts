export const PROCESS_RESULT_TYPE_CODES = [
  'collection_attachment',
  'proposal_file',
  'contract_template',
  'deliverable_report',
  'signed_contract'
] as const

const PROCESS_RESULT_TYPE_CODE_SET = new Set<string>(PROCESS_RESULT_TYPE_CODES)

export function isProcessResultTypeCode(typeCode?: string): boolean {
  if (!typeCode) return false
  return PROCESS_RESULT_TYPE_CODE_SET.has(typeCode)
}

