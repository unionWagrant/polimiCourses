#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <mpi.h>
#include <math.h>

/*
 * Group number: 38
 *
 * Group members
 *  - Fatih Temiz : 10901682
 *  - Hessamm Hashemizade: 10900041
 *  - Mehmet Emre Akbulut: 10972566
 */

const float min = 0;
const float max = 1000;
const float len = max - min;
const int num_ants = 8 * 1000 * 1000;
const int num_food_sources = 10;
const int num_iterations = 500;

float random_position() {
  return (float) rand() / (float)(RAND_MAX/(max-min)) + min;
}

/*
 * Process 0 invokes this function to initialize food sources.
 */
void init_food_sources(float* food_sources) {
  for (int i=0; i<num_food_sources; i++) {
      //make max value float

    food_sources[i] = random_position();
  }
}

/*
 * Process 0 invokes this function to initialize the position of ants.
 */
void init_ants(float* ants) {
  for (int i=0; i<num_ants; i++) {
    ants[i] = random_position();
  }
}

int main() {
  MPI_Init(NULL, NULL);
    
  int rank;
  int num_procs;
  MPI_Comm_rank(MPI_COMM_WORLD, &rank);
  MPI_Comm_size(MPI_COMM_WORLD, &num_procs);

  srand(rank);

// Allocate space in each process for food sources and ants
float *food_sources = (float *) malloc(sizeof(float) * num_food_sources);
float *local_ants = (float *) malloc(sizeof(float) * (num_ants / num_procs));

    float *global_ants = NULL;
    // Process 0 initializes food sources and ants
    if (rank== 0) {
        global_ants = (float *) malloc(sizeof(float) * num_ants);
        init_food_sources(food_sources);
        init_ants(global_ants);
    }

  // Process 0 distributed food sources and ants
    MPI_Bcast(food_sources, num_food_sources, MPI_FLOAT, 0, MPI_COMM_WORLD);
    MPI_Scatter(global_ants, num_ants / num_procs, MPI_FLOAT, local_ants, num_ants / num_procs, MPI_FLOAT, 0, MPI_COMM_WORLD);

  // Iterative simulation
  float center = 0;
  //calculate initial center of ants
  if(rank==0){
    for(int i=0; i<num_ants; i++){
      center += global_ants[i];
    }
    center = center/num_ants;
  }

  for (int iter=0; iter<num_iterations; iter++) {
      //broadcast center to all processes from process 0 (root)
      MPI_Bcast(&center, 1, MPI_FLOAT, 0, MPI_COMM_WORLD);

      //calculate new position of ants in each process for every ant
      float local_sum = 0.0;
      for(int i=0; i<num_ants / num_procs; i++){
          float ant = local_ants[i];
          float min_distance = len;
          //find closest food source
          for(int j=0; j<num_food_sources; j++){
              float food_source = food_sources[j];
              float distance = food_source - ant;
              if(fabs(distance) < fabs(min_distance)){
                  min_distance = distance;
              }
          }

            //calculate new position of ant
          float f1 = min_distance * 0.01;
          float f2 = (center - ant) * 0.012;
          ant += f1 + f2;

          //it is not possible to have position lower than min and greater than max, so we do not need to check

          // calculate local sum in each process
          local_sum = local_sum + ant;
          local_ants[i] = ant;
      }




    //reduce local sum to global sum in process 0
    float sum = 0;

    MPI_Reduce(&local_sum, &sum, 1, MPI_FLOAT, MPI_SUM, 0, MPI_COMM_WORLD);

    //calculate new center in process 0
    if (rank == 0) {
        center = sum / num_ants;
      printf("Iteration: %d - Average position: %f\n", iter, center);
    }
    MPI_Barrier(MPI_COMM_WORLD);
    }


  // Free memory
    free(food_sources);
    free(local_ants);
    if(rank==0){
        free(global_ants);
    }

  MPI_Barrier(MPI_COMM_WORLD);
  MPI_Finalize();
  return 0;
}
