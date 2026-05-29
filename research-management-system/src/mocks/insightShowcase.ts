export type DemandStatus = 'new' | 'reviewing' | 'matched' | 'in_follow_up' | 'invalid' | 'archived'
export type SourceHealth = 'healthy' | 'warning' | 'error' | 'idle'
export type TopicStage = 'emerging' | 'strong' | 'cross' | 'gap'

export interface DemandMatchMock {
  resultId: string
  resultTitle: string
  resultType: string
  owner: string
  department: string
  matchScore: number
  reason: string
  sourceSnippet: string
  updatedAt: string
  fitTags: string[]
}

export interface FollowUpStepMock {
  label: string
  owner: string
  status: 'done' | 'doing' | 'todo'
  dueAt: string
}

export interface DemandItemMock {
  id: string
  title: string
  sourceCategory: string
  sourceSite: string
  sourceUrl: string
  capturedAt: string
  industry: string
  region: string
  priority: '高' | '中' | '低'
  confidence: number
  bestMatchScore: number
  status: DemandStatus
  valueLevel: string
  owner: string
  dueAt: string
  summary: string
  llmSummary: string
  keywords: string[]
  tags: string[]
  pendingConfirmations: string[]
  riskNotes: string[]
  matches: DemandMatchMock[]
  followUp: FollowUpStepMock[]
}

export interface DemandSourceMock {
  id: string
  name: string
  type: string
  industry: string
  region: string
  frequencyHours: number
  enabled: boolean
  status: SourceHealth
  lastRunAt: string
  lastSuccessAt: string
  failureReason: string
  successRate: number
  newCount: number
  matchedCount: number
}

export interface ResearchTopicMock {
  id: string
  name: string
  domain: string
  field: string
  stage: TopicStage
  keywords: string[]
  description: string
  externalHeat: number
  internalStrength: number
  growthRate12m: number
  departments: string[]
  opportunityType: string
  recommendation: string
  explanation: string
  trend: number[]
  internalResults: string[]
  externalSignals: string[]
}

export interface ExecutiveInsightMock {
  title: string
  level: '重点关注' | '持续投入' | '组织协同' | '补齐短板'
  description: string
  evidence: string
  action: string
}

export const demandSources: DemandSourceMock[] = [
  {
    id: 'source-001',
    name: '嘉兴市科技需求发布平台',
    type: '政府/园区平台',
    industry: '综合科技',
    region: '嘉兴',
    frequencyHours: 12,
    enabled: true,
    status: 'healthy',
    lastRunAt: '2026-04-18 08:30',
    lastSuccessAt: '2026-04-18 08:31',
    failureReason: '-',
    successRate: 98,
    newCount: 18,
    matchedCount: 14
  },
  {
    id: 'source-002',
    name: '桐乡产业协同创新平台',
    type: '园区/产业平台',
    industry: '先进制造',
    region: '桐乡',
    frequencyHours: 24,
    enabled: true,
    status: 'healthy',
    lastRunAt: '2026-04-18 07:10',
    lastSuccessAt: '2026-04-18 07:11',
    failureReason: '-',
    successRate: 95,
    newCount: 12,
    matchedCount: 10
  },
  {
    id: 'source-003',
    name: '浙江政府采购公开信息',
    type: '招投标网站',
    industry: '公共服务',
    region: '浙江',
    frequencyHours: 6,
    enabled: true,
    status: 'warning',
    lastRunAt: '2026-04-18 09:00',
    lastSuccessAt: '2026-04-18 03:00',
    failureReason: '部分分页结构变更，已保留上一轮有效数据',
    successRate: 82,
    newCount: 27,
    matchedCount: 17
  },
  {
    id: 'source-004',
    name: '乌镇数字经济公开征集',
    type: '企业/专题公开页',
    industry: '数字经济',
    region: '桐乡',
    frequencyHours: 24,
    enabled: true,
    status: 'healthy',
    lastRunAt: '2026-04-17 22:00',
    lastSuccessAt: '2026-04-17 22:02',
    failureReason: '-',
    successRate: 91,
    newCount: 9,
    matchedCount: 8
  }
]

export const demandItems: DemandItemMock[] = [
  {
    id: 'demand-001',
    title: '高精度织造设备在线质量检测与缺陷识别需求',
    sourceCategory: '园区/产业平台',
    sourceSite: '桐乡产业协同创新平台',
    sourceUrl: 'https://example.com/tongxiang/textile-ai',
    capturedAt: '2026-04-18 08:42',
    industry: '先进制造',
    region: '桐乡',
    priority: '高',
    confidence: 0.92,
    bestMatchScore: 0.88,
    status: 'in_follow_up',
    valueLevel: '高价值线索',
    owner: '成果转化办公室',
    dueAt: '2026-04-23',
    summary: '企业希望在现有织造产线上引入视觉检测与缺陷识别能力，降低人工巡检成本，并对断经、污渍、纬斜等缺陷进行实时预警。',
    llmSummary: '该需求与智能制造、机器视觉、边缘计算方向高度相关，适合优先匹配已有视觉检测算法、工业相机标定、生产线数据采集类成果。',
    keywords: ['机器视觉', '缺陷识别', '纺织制造', '边缘计算'],
    tags: ['成果转化', '企业需求', '高匹配'],
    pendingConfirmations: ['确认企业现场是否已有工业相机接口', '核对缺陷样本数量和标注质量', '确认是否接受联合验证周期'],
    riskNotes: ['现场数据质量可能影响模型稳定性', '需要明确边缘设备部署环境'],
    matches: [
      {
        resultId: 'R-2025-118',
        resultTitle: '面向连续生产线的织物缺陷智能检测方法',
        resultType: '发明专利',
        owner: '陈明',
        department: '智能制造研究所',
        matchScore: 0.88,
        reason: '成果关键词与需求中的织物缺陷、在线检测、实时预警高度重合，且已有生产线验证记录。',
        sourceSnippet: '系统可针对断经、纬斜、污渍等表面缺陷进行快速定位，并输出置信度与缺陷区域。',
        updatedAt: '2026-03-26 15:20',
        fitTags: ['算法成熟', '可试点', '同城企业']
      },
      {
        resultId: 'R-2024-083',
        resultTitle: '工业视觉边缘推理网关及部署方法',
        resultType: '软件著作权',
        owner: '王蕾',
        department: '计算机学院',
        matchScore: 0.76,
        reason: '可作为产线边缘部署底座，与缺陷识别模型形成软硬件组合方案。',
        sourceSnippet: '网关支持多路视频流接入、模型热更新和本地告警推送。',
        updatedAt: '2026-02-11 11:09',
        fitTags: ['工程化能力', '边缘部署']
      }
    ],
    followUp: [
      { label: '完成需求有效性确认', owner: '科研秘书', status: 'done', dueAt: '04-18' },
      { label: '联系成果负责人确认可转化边界', owner: '成果转化办公室', status: 'doing', dueAt: '04-20' },
      { label: '组织企业技术交流', owner: '管理员', status: 'todo', dueAt: '04-23' }
    ]
  },
  {
    id: 'demand-002',
    title: '低能耗储能电池健康状态评估与安全预警技术征集',
    sourceCategory: '政府/园区平台',
    sourceSite: '嘉兴市科技需求发布平台',
    sourceUrl: 'https://example.com/jiaxing/battery-safety',
    capturedAt: '2026-04-18 07:58',
    industry: '能源储能',
    region: '嘉兴',
    priority: '高',
    confidence: 0.89,
    bestMatchScore: 0.83,
    status: 'matched',
    valueLevel: '重点跟踪',
    owner: '科研管理处',
    dueAt: '2026-04-24',
    summary: '园区企业需要对储能电池进行健康状态评估、异常温升识别和寿命预测，计划寻找算法模型和传感器融合方案。',
    llmSummary: '需求适合匹配电池管理、时序异常检测、材料老化分析和工业安全预警方向成果。',
    keywords: ['储能电池', '健康评估', '安全预警', '寿命预测'],
    tags: ['政策征集', '安全生产', '中试机会'],
    pendingConfirmations: ['确认电池类型和采样频率', '确认是否已有历史故障数据'],
    riskNotes: ['涉及安全场景，演示结果需标注辅助判断边界'],
    matches: [
      {
        resultId: 'R-2025-042',
        resultTitle: '基于多源传感数据的储能电池健康评估模型',
        resultType: '论文',
        owner: '刘洋',
        department: '新能源材料中心',
        matchScore: 0.83,
        reason: '研究对象、评估指标和预警场景均与需求一致，可作为模型验证基础。',
        sourceSnippet: '通过电压、温度、内阻等多源特征构建 SOH 估计模型，并给出异常阈值解释。',
        updatedAt: '2026-01-15 10:18',
        fitTags: ['模型基础', '安全预警']
      }
    ],
    followUp: [
      { label: '完成候选成果筛选', owner: '管理员', status: 'done', dueAt: '04-18' },
      { label: '确认企业数据样本', owner: '科研管理处', status: 'todo', dueAt: '04-22' }
    ]
  },
  {
    id: 'demand-003',
    title: '农业园区病虫害识别与精准施药轻量化应用',
    sourceCategory: '政府/园区平台',
    sourceSite: '嘉兴市科技需求发布平台',
    sourceUrl: 'https://example.com/agri/pest-detection',
    capturedAt: '2026-04-17 18:20',
    industry: '现代农业',
    region: '嘉兴',
    priority: '中',
    confidence: 0.84,
    bestMatchScore: 0.79,
    status: 'reviewing',
    valueLevel: '可培育线索',
    owner: '农业科技服务组',
    dueAt: '2026-04-26',
    summary: '园区希望通过手机拍照或巡检设备识别常见病虫害，输出防治建议，并与现有农事管理系统衔接。',
    llmSummary: '该需求与图像识别、农业知识库、轻量化模型部署方向相关，适合与已有作物病害识别成果组合。',
    keywords: ['病虫害识别', '精准施药', '农业知识库', '轻量化模型'],
    tags: ['农业场景', '移动端', '知识服务'],
    pendingConfirmations: ['确认作物类别和主要病虫害清单', '明确是否需要接入现有农事系统'],
    riskNotes: ['多作物扩展可能增加标注成本'],
    matches: [
      {
        resultId: 'R-2024-126',
        resultTitle: '面向设施农业的作物病害图像识别系统',
        resultType: '软件著作权',
        owner: '赵青',
        department: '农业信息化团队',
        matchScore: 0.79,
        reason: '成果已覆盖叶部病害图像识别，与移动端拍照识别场景契合。',
        sourceSnippet: '系统支持移动端采集、云端识别和防治知识推送。',
        updatedAt: '2025-12-03 16:40',
        fitTags: ['已有系统', '移动端']
      }
    ],
    followUp: [
      { label: '人工判断需求有效性', owner: '农业科技服务组', status: 'doing', dueAt: '04-20' },
      { label: '整理候选成果清单', owner: '科研秘书', status: 'todo', dueAt: '04-24' }
    ]
  },
  {
    id: 'demand-004',
    title: '水质监测数据异常识别和治理决策辅助模型',
    sourceCategory: '招投标网站',
    sourceSite: '浙江政府采购公开信息',
    sourceUrl: 'https://example.com/procurement/water-ai',
    capturedAt: '2026-04-17 15:36',
    industry: '生态环保',
    region: '浙江',
    priority: '中',
    confidence: 0.78,
    bestMatchScore: 0.67,
    status: 'matched',
    valueLevel: '待进一步判断',
    owner: '科研管理处',
    dueAt: '2026-04-25',
    summary: '采购公告关注水质监测时序数据异常识别、污染源推断和治理建议辅助，需要可解释模型和可视化展示。',
    llmSummary: '需求与环境监测、时序异常检测、知识图谱解释相关，现有成果匹配度中等，需要人工判断交付范围。',
    keywords: ['水质监测', '异常识别', '治理建议', '可解释模型'],
    tags: ['政府采购', '环保治理', '可解释'],
    pendingConfirmations: ['核对采购资质要求', '确认是否允许联合体参与'],
    riskNotes: ['招标周期短，需快速判断是否跟进'],
    matches: [
      {
        resultId: 'R-2023-077',
        resultTitle: '河网水质多指标异常检测与趋势分析方法',
        resultType: '论文',
        owner: '孙航',
        department: '环境工程学院',
        matchScore: 0.67,
        reason: '成果具备水质时序异常检测基础，但治理决策模块仍需补充场景规则。',
        sourceSnippet: '针对 COD、氨氮、总磷等指标构建多变量异常检测模型。',
        updatedAt: '2025-09-21 09:30',
        fitTags: ['基础匹配', '需组合方案']
      }
    ],
    followUp: [
      { label: '筛查招标资格', owner: '管理员', status: 'todo', dueAt: '04-19' },
      { label: '确认成果团队意向', owner: '科研秘书', status: 'todo', dueAt: '04-21' }
    ]
  },
  {
    id: 'demand-005',
    title: '面向数字文旅的游客行为分析与展陈内容推荐',
    sourceCategory: '企业/专题公开页',
    sourceSite: '乌镇数字经济公开征集',
    sourceUrl: 'https://example.com/wuzhen/tourism-recommendation',
    capturedAt: '2026-04-16 11:12',
    industry: '数字经济',
    region: '桐乡',
    priority: '低',
    confidence: 0.74,
    bestMatchScore: 0.58,
    status: 'new',
    valueLevel: '待研判',
    owner: '未分配',
    dueAt: '2026-04-27',
    summary: '文旅场馆希望基于游客行为数据进行人群画像、路线分析和内容推荐，提升展陈体验。',
    llmSummary: '需求与推荐系统、行为分析、人机交互相关，但与当前成果库直接匹配度偏低，可作为后续观察方向。',
    keywords: ['游客画像', '推荐系统', '数字文旅', '行为分析'],
    tags: ['观察线索', '数字服务'],
    pendingConfirmations: ['确认数据采集边界', '确认是否涉及个人信息处理'],
    riskNotes: ['需要关注数据合规与隐私授权'],
    matches: [],
    followUp: [
      { label: '分配研判负责人', owner: '管理员', status: 'todo', dueAt: '04-20' }
    ]
  }
]

export const researchTopics: ResearchTopicMock[] = [
  {
    id: 'topic-001',
    name: '工业视觉与智能制造',
    domain: '工学',
    field: '先进制造',
    stage: 'strong',
    keywords: ['机器视觉', '缺陷识别', '边缘计算', '工业质检'],
    description: '围绕生产线视觉感知、缺陷识别和边缘部署形成较稳定的成果积累。',
    externalHeat: 88,
    internalStrength: 91,
    growthRate12m: 32,
    departments: ['智能制造研究所', '计算机学院', '自动化团队'],
    opportunityType: '强势方向',
    recommendation: '优先组织企业验证和成果转化专场。',
    explanation: '内部成果基础强，外部需求热度高，适合把单点成果组合成行业解决方案。',
    trend: [42, 48, 53, 58, 62, 68, 71, 75, 82, 86, 89, 92],
    internalResults: ['织物缺陷智能检测方法', '工业视觉边缘推理网关', '生产线异常识别数据集'],
    externalSignals: ['桐乡织造企业质检需求', '园区智能工厂改造征集', '浙江制造业数字化采购公告']
  },
  {
    id: 'topic-002',
    name: '储能安全与电池健康评估',
    domain: '工学',
    field: '新能源',
    stage: 'emerging',
    keywords: ['储能电池', 'SOH评估', '热失控预警', '多源传感'],
    description: '外部需求增长快，内部已有材料与模型基础，但工程化成果仍需组织。',
    externalHeat: 86,
    internalStrength: 67,
    growthRate12m: 46,
    departments: ['新能源材料中心', '安全工程团队'],
    opportunityType: '新兴主题',
    recommendation: '建议列入重点观察方向，推动跨团队联合申报。',
    explanation: '外部安全生产和储能项目需求明显升温，内部积累中等，适合提前布局。',
    trend: [20, 24, 28, 31, 37, 42, 50, 58, 66, 73, 81, 88],
    internalResults: ['储能电池健康评估模型', '电池材料老化分析报告'],
    externalSignals: ['园区储能安全征集', '政府采购电池预警平台', '企业安全监测合作需求']
  },
  {
    id: 'topic-003',
    name: '农业 AI 与知识服务',
    domain: '农学',
    field: '智慧农业',
    stage: 'cross',
    keywords: ['病虫害识别', '精准施药', '农业知识库', '移动端识别'],
    description: '农业场景需求稳定，内部成果涉及图像识别和知识服务，具备跨学院协同价值。',
    externalHeat: 74,
    internalStrength: 72,
    growthRate12m: 24,
    departments: ['农业信息化团队', '计算机学院', '生态农业中心'],
    opportunityType: '交叉方向',
    recommendation: '建议以示范园区为载体组织联合试点。',
    explanation: '需求不一定爆发式增长，但场景清晰，适合小范围验证形成应用样板。',
    trend: [38, 40, 42, 46, 49, 51, 55, 58, 62, 65, 68, 71],
    internalResults: ['作物病害图像识别系统', '农业知识服务小程序'],
    externalSignals: ['农业园区精准施药需求', '数字乡村项目征集']
  },
  {
    id: 'topic-004',
    name: '环境监测与可解释治理模型',
    domain: '工学',
    field: '生态环保',
    stage: 'gap',
    keywords: ['水质监测', '异常检测', '污染源推断', '可解释模型'],
    description: '外部治理需求较明确，内部有检测模型基础，但决策建议与系统集成能力偏弱。',
    externalHeat: 79,
    internalStrength: 54,
    growthRate12m: 28,
    departments: ['环境工程学院', '数据科学团队'],
    opportunityType: '高潜缺口',
    recommendation: '建议补齐治理规则库和可视化解释能力后再重点推进。',
    explanation: '外部机会存在，但内部供给不足，需要先组织能力拼图。',
    trend: [30, 32, 36, 38, 42, 45, 49, 52, 57, 61, 67, 72],
    internalResults: ['河网水质异常检测方法'],
    externalSignals: ['水质治理采购公告', '生态环境监测平台升级需求']
  },
  {
    id: 'topic-005',
    name: '数字文旅与行为推荐',
    domain: '管理学',
    field: '数字服务',
    stage: 'gap',
    keywords: ['游客画像', '推荐系统', '数字展陈', '行为分析'],
    description: '外部主题热度开始出现，但内部直接成果较少，应作为观察方向。',
    externalHeat: 62,
    internalStrength: 35,
    growthRate12m: 19,
    departments: ['数字经济研究中心'],
    opportunityType: '观察缺口',
    recommendation: '暂不作为主推方向，保留数据源观察和外部合作窗口。',
    explanation: '需求存在但内部基础薄弱，适合以合作引入或小课题培育。',
    trend: [18, 19, 20, 22, 24, 27, 29, 32, 36, 39, 43, 47],
    internalResults: ['游客路线热力分析原型'],
    externalSignals: ['乌镇数字文旅内容推荐征集']
  }
]

export const executiveInsights: ExecutiveInsightMock[] = [
  {
    title: '工业视觉是当前最适合转化推进的强势方向',
    level: '持续投入',
    description: '内部成果成熟、外部企业需求集中，且与桐乡制造业场景高度贴合。',
    evidence: '近 12 个月主题热度增长 32%，4 条高质量需求中有 2 条与该方向直接相关。',
    action: '建议优先组织企业需求对接会和成果组合包展示。'
  },
  {
    title: '储能安全方向升温快，但需要跨团队组织',
    level: '重点关注',
    description: '外部政策和园区需求持续上升，内部成果分散在材料、安全和算法团队。',
    evidence: '外部热度 86，内部基础 67，主题增长率为 46%。',
    action: '建议作为下一批项目征集主题，推动联合申报。'
  },
  {
    title: '环保治理存在机会缺口，短期不宜直接承诺完整方案',
    level: '补齐短板',
    description: '内部检测模型基础可用，但治理建议、规则库和系统集成能力仍不足。',
    evidence: '外部热度 79，内部基础 54，匹配结果多为基础研究成果。',
    action: '建议先补齐可解释治理规则，再作为重点合作方向包装。'
  },
  {
    title: '农业 AI 适合做跨部门示范项目',
    level: '组织协同',
    description: '需求稳定、场景具体，适合作为交叉团队的小规模试点。',
    evidence: '参与部门 3 个，内部与外部信号均处于中高水平。',
    action: '建议选取一个农业园区做轻量化验证。'
  }
]

export const topicNetwork = {
  nodes: [
    { id: '工业视觉', name: '工业视觉', value: 92, category: '强势方向' },
    { id: '缺陷识别', name: '缺陷识别', value: 86, category: '强势方向' },
    { id: '边缘计算', name: '边缘计算', value: 70, category: '交叉技术' },
    { id: '储能安全', name: '储能安全', value: 88, category: '新兴主题' },
    { id: '健康评估', name: '健康评估', value: 76, category: '新兴主题' },
    { id: '农业AI', name: '农业AI', value: 71, category: '交叉方向' },
    { id: '知识服务', name: '知识服务', value: 58, category: '交叉方向' },
    { id: '环境监测', name: '环境监测', value: 72, category: '高潜缺口' },
    { id: '可解释模型', name: '可解释模型', value: 64, category: '高潜缺口' },
    { id: '数字文旅', name: '数字文旅', value: 47, category: '观察方向' }
  ],
  links: [
    { source: '工业视觉', target: '缺陷识别', value: 0.92 },
    { source: '工业视觉', target: '边缘计算', value: 0.74 },
    { source: '边缘计算', target: '农业AI', value: 0.46 },
    { source: '农业AI', target: '知识服务', value: 0.68 },
    { source: '储能安全', target: '健康评估', value: 0.86 },
    { source: '健康评估', target: '可解释模型', value: 0.48 },
    { source: '环境监测', target: '可解释模型', value: 0.71 },
    { source: '知识服务', target: '数字文旅', value: 0.42 }
  ]
}

export const aiAlignmentCards = [
  {
    name: '需求洞察',
    role: '成果转化主线',
    output: '需求池、匹配理由、证据片段、跟进状态',
    boundary: '辅助发现机会，不替代人工转化判断'
  },
  {
    name: '研究洞察',
    role: '管理增值能力',
    output: '热点主题、强势方向、机会缺口、建议动作',
    boundary: '辅助方向研判，不直接给出资源配置结论'
  },
  {
    name: '成果物自动补全',
    role: '录入提效能力',
    output: '预填字段、原文依据、待确认项',
    boundary: '人工确认后再写入正式表单'
  },
  {
    name: '合同识别',
    role: '过程系统提效能力',
    output: '识别摘要、关键字段、待确认项',
    boundary: '辅助审核，不替代审批结论'
  }
]
