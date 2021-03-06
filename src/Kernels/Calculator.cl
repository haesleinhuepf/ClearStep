
// You can include other resources
// Path relative to class OCLlib, the package is found automatically (first in class path if several exist)
#include [OCLlib] "linear/matrix.cl"

// You can also do absolute includes:
// Note, this is more brittle to refactoring. 
// Ideally you can move code and if the kernels 
// stay at the same location relative to the classes 
// everything is ok.
#include "clearcl/test/testinclude.cl"

// If you include something that cannot be found, 
// then it fails silently but the final source code gets annotated. 
// (check method: myprogram.getSourceCode())
#include "blu/tada.cl"

__kernel
void convert(__read_only image3d_t source,
			 __write_only image3d_t cache)
{
	int x = get_global_id(0); 
	int y = get_global_id(1);
	int z = get_global_id(2);

	int4 pos = (int4){x,y,z,0};
	
	float val = (float)READ_IMAGE(source, pos).x;
	
	write_imagef(cache, pos, (float4){val,0,0,0});
}

__kernel 
void compare(__read_only image3d_t image1, 
			 __read_only image3d_t image2, 
			 __write_only image3d_t result)
{
	int x = get_global_id(0); 
	int y = get_global_id(1);
	int z = get_global_id(2);

	int4 pos = (int4){x,y,z,0};
	
	float4 val1 = read_imagef(image1, pos);
	float4 val2 = read_imagef(image2, pos);
	
	float val = val1.x-val2.x;
		
	float4 res = (float4){(val*val),0,0,0};
	
	write_imagef(result, pos, res);
}	

__kernel
void Sum3D (__read_only image3d_t image,
            __global    float*    result,
            int blockWidthX,
            int blockWidthY,
            int blockWidthZ) 
{ 
  const int x       = get_global_id(0);
  const int y       = get_global_id(1);
  const int z       = get_global_id(2);
  
  const int nblocksx = get_global_size(0);
  const int nblocksy = get_global_size(1);
  const int nblocksz = get_global_size(2);
  
  const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP | CLK_FILTER_NEAREST;
  
  float sum = 0;
  
  const int4 origin = (int4){x*blockWidthX,y*blockWidthY,z*blockWidthZ,0};
  
  for(int lz=0; lz<blockWidthZ; lz++)
  {
    for(int ly=0; ly<blockWidthY; ly++)
    {
      for(int lx=0; lx<blockWidthX; lx++)
      {
        const int4 pos = origin + (int4){lx,ly,lz,0};
     
        float value = read_imagef(image, sampler, pos).x;

        sum = sum + value;
      }
    }
  }
  
  const int index = x+nblocksx*y+nblocksx*nblocksy*z;
  
  result[index] = sum;
}
