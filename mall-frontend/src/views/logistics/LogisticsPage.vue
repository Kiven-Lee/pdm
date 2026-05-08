<template>
  <div>
    <h2 class="page-title">物流查询</h2>

    <el-card v-if="logistics" class="logistics-card">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="快递公司">{{ logistics.company }}</el-descriptions-item>
        <el-descriptions-item label="快递单号">{{ logistics.trackingNo }}</el-descriptions-item>
        <el-descriptions-item label="物流状态">
          <el-tag :type="statusTagType(logistics.status)">{{ statusText(logistics.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="收件地址">{{ logistics.receiverAddress }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 物流轨迹时间线 -->
    <el-card class="track-card" v-if="tracks.length">
      <template #header><span>物流轨迹</span></template>
      <el-timeline>
        <el-timeline-item
          v-for="track in tracks"
          :key="track.id"
          :timestamp="track.trackTime"
          placement="top"
        >
          <el-card shadow="never" class="track-item">
            <p class="track-location">{{ track.location }}</p>
            <p class="track-remark">{{ track.remark }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-empty v-if="!logistics && !loading" description="暂无物流信息" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { logisticsApi } from '@/api'

const route = useRoute()
const logistics = ref(null)
const tracks = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await logisticsApi.getByOrderId(route.params.orderId)
    logistics.value = res.data
    if (logistics.value?.trackingNo) {
      const trackRes = await logisticsApi.getTrack(logistics.value.trackingNo)
      tracks.value = (trackRes.data || []).reverse() // 最新轨迹在前
    }
  } finally {
    loading.value = false
  }
})

function statusText(status) {
  const map = { 0: '待揽收', 1: '运输中', 2: '派送中', 3: '已签收', 4: '异常' }
  return map[status] || '未知'
}

function statusTagType(status) {
  const map = { 0: 'info', 1: 'primary', 2: 'warning', 3: 'success', 4: 'danger' }
  return map[status] || ''
}
</script>

<style scoped>
.page-title { margin-bottom: 20px; }
.logistics-card { margin-bottom: 20px; }
.track-card { margin-top: 20px; }
.track-item { background: #f9f9f9; }
.track-location { font-weight: bold; color: #333; margin-bottom: 4px; }
.track-remark { color: #666; font-size: 13px; }
</style>
