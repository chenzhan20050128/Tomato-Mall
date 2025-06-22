<template>
  <div class="ads-management">
    <div class="page-header">
      <h2>广告管理</h2>
      <el-button type="primary" @click="handleAddAd">
        <el-icon><Plus /></el-icon>
        新增广告
      </el-button>
    </div>

    <!-- 广告列表 -->
    <el-card v-loading="loading" class="ad-list-card">
      <el-table :data="advertisements" style="width: 100%">
        <el-table-column prop="id" label="ID" width="100" />
        <el-table-column label="图片" width="150">
          <template #default="scope">
            <el-image 
              :src="scope.row.imgUrl" 
              style="width: 120px; height: 80px" 
              fit="cover"
              :preview-src-list="[scope.row.imgUrl]"
              @error="handleImageError"
            />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" width="180" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
        <el-table-column label="关联商品" width="180">
          <template #default="scope">
            <div v-if="getProductById(scope.row.productId)">
              <el-tooltip placement="top">
                <template #content>
                  <div style="text-align: center;">
                    <el-image 
                      :src="getProductById(scope.row.productId)?.cover || ''" 
                      style="width: 100px; height: 100px; margin-bottom: 10px;"
                    />
                    <p>{{ getProductById(scope.row.productId)?.title }}</p>
                    <p>¥{{ getProductById(scope.row.productId)?.price }}</p>
                  </div>
                </template>
                <el-tag type="success">{{ getProductTitle(scope.row.productId) }}</el-tag>
              </el-tooltip>
            </div>
            <span v-else class="no-product">未关联商品</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" @click="handleEditAd(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDeleteAd(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="advertisements.length === 0 && !loading" class="empty-data">
        <el-empty description="暂无广告数据" />
      </div>
    </el-card>

    <!-- 添加/编辑广告对话框 -->
    <el-dialog
      :title="isEdit ? '编辑广告' : '新增广告'"
      v-model="dialogVisible"
      width="600px"
      destroy-on-close
    >
      <el-form ref="adFormRef" :model="adForm" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="adForm.title" placeholder="请输入广告标题" />
        </el-form-item>

        <el-form-item label="内容" prop="content">
          <el-input
            v-model="adForm.content"
            type="textarea"
            :rows="3"
            placeholder="请输入广告内容"
          />
        </el-form-item>

        <!-- 修改图片上传部分 -->
        <el-form-item label="图片" prop="imgUrl">
          <el-upload
            class="ad-uploader"
            action="#"
            :show-file-list="false"
            :before-upload="beforeUpload"
          >
            <img v-if="adForm.imgUrl" :src="adForm.imgUrl" class="uploaded-image" />
            <div v-else class="upload-placeholder" v-loading="uploadLoading">
              <el-icon><Plus /></el-icon>
              <div class="upload-text">点击上传图片</div>
            </div>
          </el-upload>
          <div class="upload-tip">建议尺寸: 1200px × 300px, 大小不超过2MB</div>
        </el-form-item>

      <el-form-item label="关联商品" prop="productId">
        <el-select
          v-model="adForm.productId"
          placeholder="请选择关联商品"
          filterable
          clearable
          style="width: 100%"
        >
          <el-option
            v-for="product in products"
            :key="product.id"
            :label="product.title"
            :value="product.id"
          />
        </el-select>
      </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import axios from 'axios';
import { getProductList } from '../../api/mall';
import { useRouter } from 'vue-router'
import { axios as request } from '../../utils/request';

// 定义广告类型
interface Advertisement {
  id?: string;
  title: string;
  content: string;
  imgUrl: string;
  productId?: string;
}

// 定义商品类型
interface Product {
  id: string;
  title: string;
  price: number;
  cover: string;
  description?: string;
}

// 状态变量
const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const isEdit = ref(false);
const advertisements = ref<Advertisement[]>([]);
const products = ref<Product[]>([]);
const adFormRef = ref();

// 使用可选链避免"对象可能为未定义"错误
const getProductById = (productId?: string): Product | undefined => {
  if (!productId) return undefined;
  return products.value.find(p => p.id.toString() === productId.toString());
};

const getProductTitle = (productId?: string): string => {
  const product = getProductById(productId);
  return product?.title || '未知商品';
};

// 表单数据
const adForm = reactive<Advertisement>({
  title: '',
  content: '',
  imgUrl: '',
  productId: undefined
});

// 表单校验规则
const rules = {
  title: [
    { required: true, message: '请输入广告标题', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符之间', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入广告内容', trigger: 'blur' }
  ],
  imgUrl: [
    { required: true, message: '请上传广告图片', trigger: 'change' }
  ]
};

const uploadLoading = ref(false);
// 上传相关配置
const uploadUrl = '/api/upload/image';
const uploadHeaders = {
  Authorization: `Bearer ${sessionStorage.getItem('token')}`
};

// 获取所有广告
// 然后修改函数
const fetchAdvertisements = async () => {
  loading.value = true;
  try {
    const response = await request.get('/api/advertisements');
    console.log('获取广告列表:', response.data);
    if (response.data.code === '200' || parseInt(response.data.code) === 200) {
      advertisements.value = response.data.data || [];
    } else {
      ElMessage.error(response.data.message || '获取广告列表失败');
    }
  } catch (error) {
    console.error('获取广告列表失败:', error);
    ElMessage.error('网络错误，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 获取所有商品
const fetchProducts = async () => {
  try {
    const res = await getProductList();
    if (res.data.code == 200) {
      products.value = res.data.data || [];
    } else {
      ElMessage.warning('获取商品列表失败');
    }
  } catch (error) {
    console.error('获取商品列表失败:', error);
  }
};

// 新增广告
const handleAddAd = () => {
  isEdit.value = false;
  adForm.id = undefined;
  adForm.title = '';
  adForm.content = '';
  adForm.imgUrl = '';
  adForm.productId = undefined;
  dialogVisible.value = true;
};

// 编辑广告
const handleEditAd = (ad: Advertisement) => {
  isEdit.value = true;
  adForm.id = ad.id;
  adForm.title = ad.title;
  adForm.content = ad.content;
  adForm.imgUrl = ad.imgUrl;
  adForm.productId = ad.productId;
  dialogVisible.value = true;
};



// 图片上传前的校验
const beforeUpload = async (file: File) => {
  // 检查文件类型
  const isImage = file.type.startsWith('image/');
  if (!isImage) {
    ElMessage.error('只能上传图片文件!');
    return false;
  }

  // 检查文件大小 (限制为2MB)
  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!');
    return false;
  }

  uploadLoading.value = true;

  try {
    // 创建FormData对象
    const formData = new FormData();
    formData.append('image', file);

    // 调用上传接口
    const response = await fetch('http://localhost:8080/upload', {
      method: 'POST',
      headers: {
        'token': `Bearer ${sessionStorage.getItem('token')}`
      },
      body: formData
    });

    const result = await response.json();

    if (result.code === '200') {
      // 上传成功，更新广告图片
      adForm.imgUrl = result.data;
      ElMessage.success('图片上传成功!');
    } else {
      ElMessage.error(result.msg || '图片上传失败');
    }
  } catch (error) {
    console.error('图片上传错误:', error);
    ElMessage.error('图片上传失败，请稍后重试');
  } finally {
    uploadLoading.value = false;
  }

  return false; // 阻止element-plus的默认上传行为
};



// 图片加载错误处理
const handleImageError = (e: Event) => {
  const target = e.target as HTMLImageElement;
  target.src = '/placeholder.jpg';
};

const handleDeleteAd = (ad: Advertisement) => {
  ElMessageBox.confirm(`确定要删除广告 "${ad.title}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const response = await request.delete(`/api/advertisements/${ad.id}`);
      if (response.data.code === '200' || response.data.code === 200) {
        ElMessage.success('删除成功');
        fetchAdvertisements();
      } else {
        ElMessage.error(response.data.msg || '删除失败');
      }
    } catch (error) {
      console.error('删除广告失败:', error);
      ElMessage.error('删除失败，请稍后重试');
    }
  }).catch(() => {});
};

// 提交表单
const submitForm = async () => {
  if (!adFormRef.value) return;
  
  adFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return;

    submitting.value = true;
    try {
      const method = isEdit.value ? 'put' : 'post';
      const url = '/api/advertisements';
      
      const response = await request[method](url, {
        id: adForm.id,
        title: adForm.title,
        content: adForm.content,
        imgUrl: adForm.imgUrl,
        productId: adForm.productId
      });

      if (response.data.code === '200' || response.data.code === 200) {
        ElMessage.success(isEdit.value ? '更新成功' : '添加成功');
        dialogVisible.value = false;
        fetchAdvertisements();
      } else {
        ElMessage.error(response.data.msg || (isEdit.value ? '更新失败' : '添加失败'));
      }
    } catch (error) {
      console.error(isEdit.value ? '更新广告失败:' : '添加广告失败:', error);
      ElMessage.error(isEdit.value ? '更新失败' : '添加失败');
    } finally {
      submitting.value = false;
    }
  });
};

// 组件挂载时加载数据
onMounted(() => {
  fetchAdvertisements();
  fetchProducts();
});
</script>

<style scoped>
.ads-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 22px;
  color: #303133;
  margin: 0;
}

.ad-list-card {
  margin-bottom: 20px;
}

.empty-data {
  padding: 40px 0;
}

.ad-uploader {
  width: 100%;
}

.uploaded-image {
  width: 100%;
  max-width: 300px;
  height: 150px;
  object-fit: cover;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
}

.upload-placeholder {
  width: 300px;
  height: 150px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  background-color: #fafafa;
}

.upload-placeholder .el-icon {
  font-size: 28px;
  color: #8c939d;
}

.upload-text {
  margin-top: 8px;
  font-size: 14px;
  color: #909399;
}

.product-option {
  display: flex;
  align-items: center;
  width: 100%;
}

.product-info {
  flex: 1;
  overflow: hidden;
}

.product-title {
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-price {
  font-size: 13px;
  color: #e74c3c;
}

.no-product {
  color: #909399;
  font-style: italic;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

.upload-placeholder {
  position: relative;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}
</style>