<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { axios as request } from '../../utils/request';
// 导入商品列表获取方法
import { getProductList } from '../../api/mall';

// 定义商品类型
interface Product {
  id?: string | number;
  title: string;
  author: string;
  price: number;
  category: string;
  stock: number;
  cover: string;
  description: string;
  specifications?: Array<{item: string, value: string}>;
  isAvailable?: boolean;
}

// 状态变量
const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const isEdit = ref(false);
const products = ref<Product[]>([]);
const productFormRef = ref();
const uploadLoading = ref(false);

// 分页相关
const currentPage = ref(1);
const pageSize = ref(10);
const totalItems = ref(0);

// 表单数据保持不变
const productForm = reactive<Product>({
  title: '',
  author: '',
  price: 0,
  category: '',
  stock: 0,
  cover: '',
  description: ''
});

// 表单校验规则保持不变
const rules = {
  // 保持现有规则不变
  title: [
    { required: true, message: '请输入书名', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在2到100个字符之间', trigger: 'blur' }
  ],
  author: [
    { required: true, message: '请输入作者', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' },
    { type: 'number', min: 0, message: '价格不能小于0', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  stock: [
    { required: true, message: '请输入库存', trigger: 'blur' },
    { type: 'number', min: 0, message: '库存不能小于0', trigger: 'blur' }
  ],
  cover: [
    { required: true, message: '请上传封面图片', trigger: 'change' }
  ],
  description: [
    { required: true, message: '请输入商品简介', trigger: 'blur' }
  ]
};

// 获取商品列表 - 修改为使用Mall.vue的方式
const fetchProducts = async () => {
  loading.value = true;
  try {
    const res = await getProductList();
    if (res.data.code == 200) {
      products.value = res.data.data || [];
      console.log('获取商品列表成功:', products.value);
      // 补充可能缺失的字段
      products.value.forEach(product => {
        if (product.stock === undefined) {
          product.stock = 100;
        }
        if (product.isAvailable === undefined) {
          product.isAvailable = true;
        }
        
        // 从specifications中提取作者信息
        if (!product.author && product.specifications) {
          const authorSpec = product.specifications.find(spec => spec.item === '作者');
          if (authorSpec) {
            product.author = authorSpec.value;
          }
        }
        
        // 从specifications中提取分类信息
        if (!product.category && product.specifications) {
          const categorySpec = product.specifications.find(spec => spec.item === '分类');
          if (categorySpec) {
            product.category = categorySpec.value;
          }
        }
      });
      
      // 更新总数
      totalItems.value = products.value.length;
    } else {
      ElMessage.error('获取商品列表失败');
    }
  } catch (error) {
    console.error('获取商品列表失败:', error);
    ElMessage.error('网络错误，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 添加计算属性实现前端分页
const paginatedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return products.value.slice(start, end);
});

// 修改分页处理逻辑为前端分页
const handleSizeChange = (val: number) => {
  pageSize.value = val;
  currentPage.value = 1;
};

const handleCurrentChange = (val: number) => {
  currentPage.value = val;
};

// 其他函数保持不变...
const handleAddProduct = () => {
  isEdit.value = false;
  Object.assign(productForm, {
    id: undefined,
    title: '',
    author: '',
    price: 0,
    category: '',
    stock: 0,
    cover: '',
    description: ''
  });
  dialogVisible.value = true;
};

const handleEditProduct = (product: Product) => {
  isEdit.value = true;
  
  // 从specifications中提取作者和分类，或使用当前值
  const author = getAuthorFromSpecs(product) || product.author || '';
  const category = getCategoryFromSpecs(product) || product.category || '';
  
  Object.assign(productForm, {
    ...product,
    author,
    category
  });
  
  dialogVisible.value = true;
};

const handleDeleteProduct = (product: Product) => {
  ElMessageBox.confirm(`确定要删除商品"${product.title}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const response = await request.delete(`/api/products/${product.id}`);
      if (response.data.code === '200' || response.data.code === 200) {
        ElMessage.success('删除成功');
        fetchProducts(); // 重新获取所有商品
      } else {
        ElMessage.error(response.data.msg || '删除失败');
      }
    } catch (error) {
      console.error('删除商品失败:', error);
      ElMessage.error('删除失败，请稍后重试');
    }
  }).catch(() => {});
};

// 图片上传相关代码保持不变...
const beforeUpload = async (file: File) => {
  const isImage = file.type.startsWith('image/');
  if (!isImage) {
    ElMessage.error('只能上传图片文件!');
    return false;
  }

  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过2MB!');
    return false;
  }

  uploadLoading.value = true;

  try {
    const formData = new FormData();
    formData.append('image', file);

    const response = await fetch('http://localhost:8080/upload', {
      method: 'POST',
      headers: {
        'token': `Bearer ${sessionStorage.getItem('token')}`
      },
      body: formData
    });

    const result = await response.json();

    if (result.code === '200') {
      productForm.cover = result.data;
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

  return false;
};

// 图片加载错误处理
const handleImageError = (e: Event) => {
  const target = e.target as HTMLImageElement;
  target.src = '/placeholder.jpg';
};

// 提交表单保持不变...
const submitForm = async () => {
  if (!productFormRef.value) return;
  
  productFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return;

    submitting.value = true;
    try {
      const method = isEdit.value ? 'put' : 'post';
      const url = '/api/products';
      
      // 构造后端期望的数据格式，添加rate字段
      const submitData = {
        id: productForm.id,
        title: productForm.title,
        price: productForm.price,
        description: productForm.description,
        cover: productForm.cover,
        stock: productForm.stock,
        // 添加rate字段，设置默认值为5.0
        rate: 5.0,
        specifications: [
          {
            item: "作者",
            value: productForm.author
          },
          {
            item: "分类",
            value: productForm.category
          }
        ]
      };

      console.log('提交的商品数据:', submitData);
      
      const response = await request[method](url, submitData);

      if (response.data.code === '200' || response.data.code === 200) {
        ElMessage.success(isEdit.value ? '更新成功' : '添加成功');
        dialogVisible.value = false;
        fetchProducts();
      } else {
        ElMessage.error(response.data.msg || (isEdit.value ? '更新失败' : '添加失败'));
      }
    } catch (error) {
      console.error(isEdit.value ? '更新商品失败:' : '添加商品失败:', error);
      ElMessage.error(isEdit.value ? '更新失败' : '添加失败');
    } finally {
      submitting.value = false;
    }
  });
};

// 辅助函数 - 从规格中提取作者和分类
const getAuthorFromSpecs = (product: Product): string => {
  if (product.specifications) {
    const authorSpec = product.specifications.find(spec => spec.item === '作者');
    return authorSpec ? authorSpec.value : '';
  }
  return '';
};

const getCategoryFromSpecs = (product: Product): string => {
  if (product.specifications) {
    const categorySpec = product.specifications.find(spec => spec.item === '分类');
    return categorySpec ? categorySpec.value : '';
  }
  return '';
};

// 组件挂载时加载数据
onMounted(() => {
  fetchProducts();
});
</script>

<template>
  <!-- 页面结构保持不变，只修改数据源为paginatedProducts -->
  <div class="product-management">
    <div class="page-header">
      <h2>商品管理</h2>
      <el-button type="primary" @click="handleAddProduct">
        <el-icon><Plus /></el-icon>
        添加商品
      </el-button>
    </div>

    <!-- 商品列表 -->
    <el-card v-loading="loading" class="product-list-card">
      <el-table :data="paginatedProducts" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="封面" width="120">
          <template #default="scope">
            <el-image 
              :src="scope.row.cover" 
              style="width: 100px; height: 120px" 
              fit="cover"
              :preview-src-list="[scope.row.cover]"
              @error="handleImageError"
            />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="书名" width="180" show-overflow-tooltip />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="scope">
            ¥{{ scope.row.price }}
          </template>
        </el-table-column>
        <el-table-column label="作者" width="120" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.author || getAuthorFromSpecs(scope.row) || '未知' }}
          </template>
        </el-table-column>
        <el-table-column label="分类" width="100">
          <template #default="scope">
            {{ scope.row.category || getCategoryFromSpecs(scope.row) || '未分类' }}
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column prop="description" label="简介" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" @click="handleEditProduct(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDeleteProduct(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="products.length === 0 && !loading" class="empty-data">
        <el-empty description="暂无商品数据" />
      </div>
      
      <!-- 分页控件保持不变，但现在使用前端分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:currentPage="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalItems"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 对话框内容保持不变 -->
    <el-dialog
      :title="isEdit ? '编辑商品' : '添加商品'"
      v-model="dialogVisible"
      width="650px"
      destroy-on-close
    >
      <!-- 表单内容保持不变 -->
      <el-form ref="productFormRef" :model="productForm" :rules="rules" label-width="100px">
        <!-- 表单项保持不变 -->
        <el-form-item label="书名" prop="title">
          <el-input v-model="productForm.title" placeholder="请输入书名" />
        </el-form-item>

        <el-form-item label="作者" prop="author">
          <el-input v-model="productForm.author" placeholder="请输入作者" />
        </el-form-item>

        <el-form-item label="价格" prop="price">
          <el-input-number v-model="productForm.price" :precision="2" :step="0.01" :min="0" />
        </el-form-item>

        <el-form-item label="分类" prop="category">
          <el-select v-model="productForm.category" placeholder="请选择分类">
            <el-option label="小说" value="小说" />
            <el-option label="文学" value="文学" />
            <el-option label="计算机" value="计算机" />
            <el-option label="教育" value="教育" />
            <el-option label="历史" value="历史" />
            <el-option label="科幻" value="科幻" />
            <el-option label="经济管理" value="经济管理" />
            <el-option label="儿童读物" value="儿童读物" />
          </el-select>
        </el-form-item>

        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="productForm.stock" :min="0" :step="1" />
        </el-form-item>

        <el-form-item label="封面图片" prop="cover">
          <el-upload
            class="product-uploader"
            action="#"
            :show-file-list="false"
            :before-upload="beforeUpload"
          >
            <img v-if="productForm.cover" :src="productForm.cover" class="uploaded-image" />
            <div v-else class="upload-placeholder" v-loading="uploadLoading">
              <el-icon><Plus /></el-icon>
              <div class="upload-text">点击上传封面</div>
            </div>
          </el-upload>
          <div class="upload-tip">建议尺寸: 800px × 1000px, 大小不超过2MB</div>
        </el-form-item>

        <el-form-item label="简介" prop="description">
          <el-input
            v-model="productForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入商品简介"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitting">
            {{ isEdit ? '保存' : '创建' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.product-management {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.product-list-card {
  margin-bottom: 24px;
}

.empty-data {
  padding: 40px 0;
  text-align: center;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* 上传组件样式 */
.product-uploader {
  width: 100%;
}

.uploaded-image {
  width: 200px;
  height: 250px;
  object-fit: cover;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
}

.upload-placeholder {
  width: 200px;
  height: 250px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  background-color: #fafafa;
  position: relative;
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

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>