package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品，同时保存对应的口味数据
     * @param dishDTO
     */
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {

        // Dish 的字段对应数据库的 dish 表字段，DishDTO 除了 Dish 的字段，还有 flavors (List 类型)
        // 将 DishDTO 的属性复制给 Dish 对象，这样插入的就只有 Dish，没有 flavors
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.insert(dish);

        //获取insert语句生成的主键值 （Dish和 flavor是一对多的关系，一个菜品对应多个口味，外键是dishId）
        Long dishId = dish.getId();

        //从前端传过来的 DishDTO 里，取出用户填写的所有菜品口味，存到口味集合 flavors 中
        //再挨个取出集合里每一条口味，给每一个口味对象都设置上刚刚新增菜品的 id （标记这条口味属于哪一道菜）
        // 最后，把所有已经绑定好菜品 id 的口味，批量一次性插入口味数据表

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){

            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavors);
            //保存菜品
        }
    }

    /**
     * 分页查询菜品
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除菜品
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids){
        // 起售中的菜品不能删除
            for (Long id : ids) {
                Dish dish = dishMapper.getById(id);
                if (dish.getStatus() == StatusConstant.ENABLE) {
                    throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
                }
            }

            // 判断当前菜品是否关联到套餐
           // setmealDish表是中间表，字段由id, setmeal_id,dish_id, name, price,copies组成
          // 所以只需要判断下 当前的这些dish_id是否存在在setmealDish表中即可

        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
            if (setmealIds != null && setmealIds.size() > 0) {
                // 当前菜品有在套餐中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
            }

            // 删除菜品数据和风味数据
        for (Long id : ids){
            dishMapper.deleteById(id);
            dishFlavorMapper.deleteByDishId(id);
        }
    }

    /**
     * 根据id查询菜品和对应的口味数据
     * @param id
     * @return
     */
   public DishVO getByIdWithFlavor(Long id) {
       Dish dish = dishMapper.getById(id);
       List<DishFlavor> dishFlavors =  dishFlavorMapper.getByDishId(id);

       // 将查询到的数据封装到 DishVO 中
       DishVO dishVO = new DishVO();
       BeanUtils.copyProperties(dish,dishVO);

       //把口味列表数据set到 VO中（VO有一个字段是List<DishFlavor> flavors）
       dishVO.setFlavors(dishFlavors);
       return dishVO;
   }

    /**
     * 根据 dishId 修改菜品和口味
     */
    @Transactional
   public void updateWithFlavor(DishDTO dishDTO){
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        dishMapper.update(dish);

        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 根据dishId 起售和停售菜品
     */
    @Transactional
   public void StartOrStop(Integer status, Long id){
       Dish dish = Dish.builder().id(id).status(status).build();
       dishMapper.update(dish);

       //如果菜品停售,则关联的多个套餐也要停售
       if(status == StatusConstant.DISABLE){
           List<Long> dishIds = new ArrayList<>();
           dishIds.add(id);
           //根据菜品id 查setmealId (套餐 id)
           List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(dishIds);
           if(setmealIds != null && setmealIds.size() > 0){
               for(Long setmealId : setmealIds){
                   Setmeal setmeal = Setmeal.builder().id(id).status(StatusConstant.DISABLE).build();
                   setmealDishMapper.update(setmeal);
               }
           }
       }
    }
}
