import numpy 
import math

b = numpy.array([1.2041199827, 0, 1.2041199827, 0, 0, 0, 0, 0])
d = numpy.array([0.6020599913, 0, 0.6020599913, 0, 0.9542425094, 0, 0.9542425094, 0])

print(b)
print(d)

b_mag = math.sqrt(numpy.sum(numpy.square(b)))
d_mag = math.sqrt(numpy.sum(numpy.square(d)))

print(b_mag)
print(d_mag)

b_prime = numpy.true_divide(b, b_mag)
d_prime = numpy.true_divide(d, d_mag)

print(b_prime)
print(d_prime)

diff = numpy.subtract(b_prime, d_prime)

print(numpy.square(diff))

value = numpy.sum(numpy.square(diff))

print(value)

similarity = math.sqrt(value)

print("Similarity", similarity)
print(math.sqrt(numpy.sum(numpy.square(b_prime))))
print(math.sqrt(numpy.sum(numpy.square(d_prime))))