// 基础模拟数据，用于前端演示
const nowYear = new Date().getFullYear()

export const users = [
  {
    id: '1',
    username: 'admin',
    password: 'admin123',
    name: '管理员',
    email: 'admin@example.com',
    role: 'admin',
    department: '科研管理部',
    avatar: '',
    token: 'mock-admin-token'
  },
  {
    id: '2',
    username: 'expert',
    password: 'expert123',
    name: '审核专家',
    email: 'expert@example.com',
    role: 'expert',
    department: '材料学院',
    avatar: '',
    token: 'mock-expert-token'
  },
  {
    id: '3',
    username: 'researcher',
    password: 'researcher123',
    name: '科研用户',
    email: 'researcher@example.com',
    role: 'researcher',
    department: '计算机学院',
    avatar: '',
    token: 'mock-researcher-token'
  }
]

export const resultTypes = [
  {
    id: 'paper',
    name: '论文',
    code: 'paper',
    description: '学术期刊或会议论文',
    enabled: true,
    fields: [
      { id: 'journal', name: 'journal', label: '期刊/会议', type: 'text', required: true, order: 1 },
      { id: 'doi', name: 'doi', label: 'DOI', type: 'text', required: false, order: 2 },
      { id: 'impact', name: 'impact', label: '影响因子', type: 'number', required: false, order: 3 }
    ]
  },
  {
    id: 'patent',
    name: '专利',
    code: 'patent',
    description: '专利及授权',
    enabled: true,
    fields: [
      { id: 'patentNo', name: 'patentNo', label: '专利号', type: 'text', required: true, order: 1 },
      { id: 'owner', name: 'owner', label: '权利人', type: 'text', required: false, order: 2 }
    ]
  }
]

export const projects = [
  {
    id: 'p-001',
    name: '智慧医疗专项',
    code: 'MED-2024',
    description: '面向智慧医疗的关键技术研究'
  },
  {
    id: 'p-002',
    name: '储能技术基础研究',
    code: 'EN-BASE-2023',
    description: '下一代高比能储能材料与装置'
  },
  {
    id: 'p-003',
    name: '轨道交通专项',
    code: 'RAIL-2022',
    description: '高速列车空气动力学与结构优化'
  }
]

export const results = [
  {
    id: 'r-001',
    title: '深度学习在医学影像中的应用',
    type: 'paper',
    typeId: 'paper',
    authors: ['科研用户', '审核专家'],
    year: nowYear,
    abstract: '探讨深度学习在医学影像分割与诊断中的关键技术与挑战。',
    keywords: ['深度学习', '医学影像', '分割'],
    projectId: 'p-001',
    projectName: '智慧医疗专项',
    projectCode: 'MED-2024',
    status: 'published',
    visibility: 'public_abstract',
    attachments: [
      { id: 'att-1', name: '论文全文.pdf', size: 1024 * 600, type: 'pdf', url: '/mock/paper.pdf', uploadedAt: '2024-01-12' }
    ],
    metadata: { journal: 'Medical Imaging Journal', doi: '10.1000/mock-doi-1', impact: 5.1 },
    createdBy: '科研用户',
    createdAt: '2024-01-12',
    updatedAt: '2024-02-02',
    reviewHistory: [
      { id: 'rev-1', reviewerId: '2', reviewerName: '审核专家', action: 'approve', comment: '内容完整，建议公开摘要。', createdAt: '2024-01-20' }
    ]
  },
  {
    id: 'r-002',
    title: '下一代电池材料性能优化',
    type: 'paper',
    typeId: 'paper',
    authors: ['科研用户'],
    year: nowYear - 1,
    abstract: '针对高比能电池材料，研究新型合成方法与性能提升策略。',
    keywords: ['电池', '材料', '性能优化'],
    projectId: 'p-002',
    projectName: '储能技术基础研究',
    projectCode: 'EN-BASE-2023',
    status: 'reviewing',
    visibility: 'internal_full',
    attachments: [],
    metadata: { journal: 'Energy Materials', doi: '10.1000/mock-doi-2', impact: 7.8 },
    createdBy: '科研用户',
    createdAt: '2023-11-05',
    updatedAt: '2023-12-01',
    reviewHistory: []
  },
  {
    id: 'r-003',
    title: '高速列车空气动力学专利',
    type: 'patent',
    typeId: 'patent',
    authors: ['审核专家'],
    year: nowYear - 2,
    abstract: '涉及高速列车头型的空气动力学优化设计与结构改进。',
    keywords: ['高速列车', '空气动力学', '专利'],
    projectId: 'p-003',
    projectName: '轨道交通专项',
    projectCode: 'RAIL-2022',
    status: 'pending',
    visibility: 'internal_abstract',
    attachments: [],
    metadata: { patentNo: 'CN-2022-123456', owner: '审核专家' },
    createdBy: '审核专家',
    createdAt: '2022-08-10',
    updatedAt: '2022-08-11',
    reviewHistory: []
  },
  {
    id: 'r-004',
    title: '未归属项目的预研成果示例',
    type: 'paper',
    typeId: 'paper',
    authors: ['科研用户'],
    year: nowYear,
    abstract: '用于验证无项目/其他场景下的成果创建与展示逻辑。',
    keywords: ['预研', '示例'],
    projectId: '',
    projectName: '',
    projectCode: '',
    status: 'draft',
    visibility: 'private',
    attachments: [],
    metadata: { journal: '预研内部刊物', doi: '', impact: null },
    createdBy: '科研用户',
    createdAt: '2024-02-01',
    updatedAt: '2024-02-01',
    reviewHistory: []
  }
]

export const uploads = []

export const demands = [
  {
    id: 'd-001',
    title: '三甲医院希望提升放射科影像诊断效率，减少人工标注负担',
    summary: '某三甲医院计划在放射科引入智能影像辅助诊断，提高分割与检出效率，并降低医师重复工作量。',
    llmSummary: '医院需要引入医疗影像AI，重点在分割、检测、效率提升，关注合规与部署。',
    keywords: ['医疗影像', 'AI分割', '效率'],
    tags: ['医疗', '效率提升'],
    industry: '医疗健康',
    region: '北京',
    sourceCategory: '政府/园区平台',
    sourceSite: 'HospitalTech Forum',
    sourceUrl: 'https://example.com/hospital/need-1',
    capturedAt: '2025-01-12',
    confidence: 0.82,
    status: 'matched',
    bestMatchScore: 0.86,
    matches: [
      {
        resultId: 'r-001',
        resultTitle: '深度学习在医学影像中的应用',
        resultType: '论文',
        owner: '科研用户',
        matchScore: 0.86,
        reason: '需求中的“影像分割”“诊断效率”与成果摘要高相似度，关键词重叠。',
        sourceSnippet: '希望在放射科引入智能影像分割与检测，减少医生重复标注工作。',
        updatedAt: '2025-01-13'
      }
    ]
  },
  {
    id: 'd-002',
    title: '新能源企业需要改进电池热管理方案，解决高倍率充放电发热问题',
    summary: '一家动力电池企业在高倍率工况下遇到热失控风险，寻求材料与散热结构优化方案。',
    llmSummary: '电池热管理与材料改性需求，方向包含材料导热性提升、结构散热优化。',
    keywords: ['电池', '热管理', '高倍率'],
    tags: ['新能源', '安全'],
    industry: '能源储能',
    region: '深圳',
    sourceCategory: '企业官网/年报',
    sourceSite: 'EnergyHub',
    sourceUrl: 'https://example.com/energy/need-1',
    capturedAt: '2025-01-10',
    confidence: 0.77,
    status: 'matched',
    bestMatchScore: 0.72,
    matches: [
      {
        resultId: 'r-002',
        resultTitle: '下一代电池材料性能优化',
        resultType: '论文',
        owner: '科研用户',
        matchScore: 0.72,
        reason: '涉及高比能电池材料与性能提升，与热管理改性方向契合度较高。',
        sourceSnippet: '高倍率下发热严重，需通过材料与结构优化降低热失控风险。',
        updatedAt: '2025-01-12'
      }
    ]
  },
  {
    id: 'd-003',
    title: '轨交车辆制造商寻找降阻车头设计方案，降低能耗',
    summary: '轨道交通制造企业希望在下一代动车组中采用更优的空气动力学车头设计以降低阻力与能耗。',
    keywords: ['空气动力学', '降阻', '轨交'],
    tags: ['交通', '节能'],
    industry: '轨道交通',
    region: '上海',
    sourceCategory: '招投标网站',
    sourceSite: 'RailwayWeekly',
    sourceUrl: 'https://example.com/rail/need-1',
    capturedAt: '2025-01-08',
    confidence: 0.7,
    status: 'unmatched',
    bestMatchScore: 0.32,
    matches: [
      {
        resultId: 'r-003',
        resultTitle: '高速列车空气动力学专利',
        resultType: '专利',
        owner: '审核专家',
        matchScore: 0.32,
        reason: '车头空气动力学优化方向相关，但需求侧有定制化要求，需进一步验证。',
        sourceSnippet: '希望降低列车运行阻力与能耗，寻找更流线的车头结构设计。',
        updatedAt: '2025-01-09'
      }
    ]
  }
]

export function nextId(prefix = 'r') {
  return `${prefix}-${Math.random().toString(16).slice(2, 8)}`
}
