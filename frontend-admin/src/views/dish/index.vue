<template>
  <div class="dashboard-container">
    <div class="container">
      <div class="tableBar">
        <label style="margin-right: 10px">菜品名称：</label>
        <el-input v-model="input"
                  placeholder="请填写菜品名称"
                  style="width: 14%"
                  clearable
                  @clear="init"
                  @keyup.enter.native="initFun" />

        <label style="margin-right: 10px; margin-left: 20px">菜品分类：</label>
        <el-select v-model="categoryId"
                   style="width: 14%"
                   placeholder="请选择"
                   clearable
                   @clear="init">
          <el-option v-for="item in dishCategoryList"
                     :key="item.value"
                     :label="item.label"
                     :value="item.value" />
        </el-select>

        <label style="margin-right: 10px; margin-left: 20px">售卖状态：</label>
        <el-select v-model="dishStatus"
                   style="width: 14%"
                   placeholder="请选择"
                   clearable
                   @clear="init">
          <el-option v-for="item in saleStatus"
                     :key="item.value"
                     :label="item.label"
                     :value="item.value" />
        </el-select>
        <el-button class="normal-btn continue"
                   @click="init(true)">
          查询
        </el-button>

        <div class="tableLab">
          <span class="delBut non"
                @click="deleteHandle('批量', null)">批量删除</span>
          <!-- <span class="blueBug non" @click="statusHandle('1')">批量启售</span>
          <span
            style="border: none"
            class="delBut non"
            @click="statusHandle('0')"
            >批量停售</span
          > -->
          <el-button type="primary"
                     style="margin-left: 15px"
                     @click="addDishtype('add')">
            + 新建菜品
          </el-button>
        </div>
      </div>
      <el-table v-if="tableData.length"
                :data="tableData"
                stripe
                class="tableBox"
                @selection-change="handleSelectionChange">
        <el-table-column type="selection"
                         width="25" />
        <el-table-column prop="name"
                         label="菜品名称" />
        <el-table-column prop="image"
                         label="图片">
          <template slot-scope="{ row }">
            <el-image style="width: 80px; height: 40px; border: none; cursor: pointer"
                      :src="row.image">
              <div slot="error"
                   class="image-slot">
                <img src="./../../assets/noImg.png"
                     style="width: auto; height: 40px; border: none">
              </div>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName"
                         label="菜品分类" />
        <el-table-column label="售价">
          <template slot-scope="scope">
            <span style="margin-right: 10px">￥{{ (scope.row.price ).toFixed(2)*100/100 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="售卖状态">
          <template slot-scope="scope">
            <div class="tableColumn-status"
                 :class="{ 'stop-use': String(scope.row.status) === '0' }">
              {{ String(scope.row.status) === '0' ? '停售' : '启售' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime"
                         label="最后操作时间" />
        <el-table-column label="操作"
                         width="300"
                         align="center">
          <template slot-scope="scope">
            
            <el-button type="text"
                       size="small"
                       class="blueBug"
                       @click="addDishtype(scope.row.id)">
              修改
            </el-button>

            <el-button type="text"
                       size="small"
                       class="blueBug"
                       @click="openSeckill(scope.row)">
              设置秒杀
            </el-button>

            <el-button type="text"
                       size="small"
                       class="delBut"
                       @click="deleteHandle('单删', scope.row.id)">
              删除
            </el-button>

            <el-button type="text"
                       size="small"
                       class="non"
                       :class="{
                         blueBug: scope.row.status == '0',
                         delBut: scope.row.status != '0'
                       }"
                       @click="statusHandle(scope.row)">
              {{ scope.row.status == '0' ? '启售' : '停售' }}
            </el-button>

          </template>
        </el-table-column>
      </el-table>
      <Empty v-else
             :is-search="isSearch" />
      <el-pagination v-if="counts > 10"
                     class="pageList"
                     :page-sizes="[10, 20, 30, 40]"
                     :page-size="pageSize"
                     layout="total, sizes, prev, pager, next, jumper"
                     :total="counts"
                     @size-change="handleSizeChange"
                     @current-change="handleCurrentChange" />
      <el-dialog
        title="🔥 设置秒杀活动"
        :visible.sync="seckillDialogVisible"
        width="30%"
        append-to-body
      >
        <el-form label-width="100px">
          <el-form-item label="当前菜品">
            <span style="font-weight: bold">{{ seckillForm.name }}</span>
          </el-form-item>
          
          <el-form-item label="秒杀价格">
            <el-input-number v-model="seckillForm.price" :min="0" :precision="2" label="元"></el-input-number>
          </el-form-item>

          <el-form-item label="秒杀库存">
            <el-input-number v-model="seckillForm.stock" :min="1" :max="999" label="份"></el-input-number>
          </el-form-item>

        

        <el-form-item label="活动时间">
          <el-date-picker
            v-model="seckillForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        </el-form>

        <span slot="footer" class="dialog-footer">
          <el-button @click="seckillDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="confirmSeckill">确 定</el-button>
        </span>
      </el-dialog>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator'
import HeadLable from '@/components/HeadLable/index.vue'
import {
  getDishPage,
  editDish,
  deleteDish,
  dishStatusByStatus,
  dishCategoryList,
  addSeckill
} from '@/api/dish'
import InputAutoComplete from '@/components/InputAutoComplete/index.vue'
import Empty from '@/components/Empty/index.vue'
import { baseUrl } from '@/config.json'

@Component({
  name: 'DishType',
  components: {
    HeadLable,
    InputAutoComplete,
    Empty
  }
})
export default class extends Vue {
  private seckillDialogVisible: boolean = false
  private seckillForm = {
    id: 0,
    name: '',
    price: 1,
    stock: 100,
    timeRange: [] as string[]
  }

  // 打开弹窗
  private openSeckill(row: any) {
    this.seckillForm.id = row.id
    this.seckillForm.name = row.name
    this.seckillForm.price = 0.01 // 默认秒杀价
    this.seckillForm.stock = 100  // 默认库存
    this.seckillDialogVisible = true
  }

  // 确认提交（演示版）
  private async confirmSeckill() {
    // 1. 校验时间是否选择了
    if (!this.seckillForm.timeRange || this.seckillForm.timeRange.length < 2) {
      this.$message.error('请选择秒杀活动的起止时间！')
      return
    }

    // 2. 构造传给后端的参数
    const params = {
      dishId: this.seckillForm.id,      // 对应 seckill_dish 表的 dish_id
      seckillPrice: this.seckillForm.price,
      stockCount: this.seckillForm.stock,
      startTime: this.seckillForm.timeRange[0], // 拆分出开始时间
      endTime: this.seckillForm.timeRange[1]    // 拆分出结束时间
    }

    // 3. 发送请求
    try {
      const res: any = await addSeckill(params) // 注意这里传 params，而不是 this.seckillForm
      if (res.data.code === 1) {
        this.$message.success('设置秒杀成功！')
        this.seckillDialogVisible = false
        // this.init() // 这里不需要刷新菜品列表，因为这是独立的秒杀表
      } else {
        this.$message.error(res.data.msg || '设置失败')
      }
    } catch (err) {
      this.$message.error('请求出错了：' + err.message)
    }
  }
  private input: any = ''
  private counts: number = 0
  private page: number = 1
  private pageSize: number = 10
  private checkList: string[] = []
  private tableData: [] = []
  private dishState = ''
  private dishCategoryList = []
  private categoryId = ''
  private dishStatus = ''
  private isSearch: boolean = false
  private saleStatus: any = [
    {
      value: 0,
      label: '停售'
    },
    {
      value: 1,
      label: '启售'
    }
  ]

  created() {
    this.init()
    this.getDishCategoryList()
  }

  initProp(val) {
    this.input = val
    this.initFun()
  }

  initFun() {
    this.page = 1
    this.init()
  }

 private init(isSearch?: boolean) {
    this.isSearch = isSearch || false;
    getDishPage({
      page: this.page,
      pageSize: this.pageSize,
      name: this.input || undefined,
      categoryId: this.categoryId || undefined,
      status: this.dishStatus
    })
    .then(res => {
      // 这里的逻辑直接包裹在 then 里面
      if (res && res.data && res.data.code === 1) {
        this.tableData = res.data.data.records || [];
        this.counts = Number(res.data.data.total) || 0;
      } else {
        this.$message.error((res && res.data && res.data.msg) || '获取列表失败');
      }
    })
    .catch(err => {
      // 明确写出 catch 逻辑，防止 Promise 链条悬空
      console.error('查询异常:', err);
    });
  }

  // 添加
  private addDishtype(st: string) {
    if (st === 'add') {
      this.$router.push({ path: '/dish/add' })
    } else {
      this.$router.push({ path: '/dish/add', query: { id: st } })
    }
  }

  // 删除
  private deleteHandle(type: string, id: any) {
    if (type === '批量' && id === null) {
      if (this.checkList.length === 0) {
        return this.$message.error('请选择删除对象')
      }
    }
    this.$confirm('确认删除该菜品, 是否继续?', '确定删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      deleteDish(type === '批量' ? this.checkList.join(',') : id)
        .then(res => {
          if (res && res.data && res.data.code === 1) {
            this.$message.success('删除成功！')
            this.init()
          } else {
            this.$message.error(res.data.msg)
          }
        })
        .catch(err => {
          this.$message.error('请求出错了：' + err.message)
        })
    })
  }
  //获取菜品分类下拉数据
  private getDishCategoryList() {
    dishCategoryList({
      type: 1
    })
      .then(res => {
        if (res && res.data && res.data.code === 1) {
          this.dishCategoryList = (
            res.data &&
            res.data.data &&
            res.data.data
          ).map(item => {
            return { value: item.id, label: item.name }
          })
        }
      })
      .catch(() => {})
  }

  //状态更改
  private statusHandle(row: any) {
    let params: any = {}
    if (typeof row === 'string') {
      if (this.checkList.length === 0) {
        this.$message.error('批量操作，请先勾选操作菜品！')
        return false
      }
      params.id = this.checkList.join(',')
      params.status = row
    } else {
      params.id = row.id
      params.status = row.status ? '0' : '1'
    }
    this.dishState = params
    this.$confirm('确认更改该菜品状态?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      // 起售停售---批量起售停售接口
      dishStatusByStatus(this.dishState)
        .then(res => {
          if (res && res.data && res.data.code === 1) {
            this.$message.success('菜品状态已经更改成功！')
            this.init()
          } else {
            this.$message.error(res.data.msg)
          }
        })
        .catch(err => {
          this.$message.error('请求出错了：' + err.message)
        })
    })
  }

  // 全部操作
  private handleSelectionChange(val: any) {
    let checkArr: any[] = []
    val.forEach((n: any) => {
      checkArr.push(n.id)
    })
    this.checkList = checkArr
  }

  private handleSizeChange(val: any) {
    this.pageSize = val
    this.init()
  }

  private handleCurrentChange(val: any) {
    this.page = val
    this.init()
  }
}
</script>
<style lang="scss">
.el-table-column--selection .cell {
  padding-left: 10px;
}
</style>
<style lang="scss" scoped>
.dashboard {
  &-container {
    margin: 30px;
    .container {
      background: #fff;
      position: relative;
      z-index: 1;
      padding: 30px 28px;
      border-radius: 4px;
      //查询黑色按钮样式
      .normal-btn {
        background: #333333;
        color: white;
        margin-left: 20px;
      }
      .tableBar {
        margin-bottom: 20px;

        .tableLab {
          display: inline-block;
          float: right;
          span {
            cursor: pointer;
            display: inline-block;
            font-size: 14px;
            padding: 0 20px;
            color: $gray-2;
          }
        }
      }
      .tableBox {
        width: 100%;
        border: 1px solid $gray-5;
        border-bottom: 0;
      }
      .pageList {
        text-align: center;
        margin-top: 30px;
      }
    }
  }
}
</style>
