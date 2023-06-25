# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: model_package_storage.proto
"""Generated protocol buffer code."""
from google.protobuf.internal import enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()




DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(b'\n\x1bmodel_package_storage.proto\"\x91\x02\n\x04\x46ile\x12\x17\n\x04type\x18\x01 \x01(\x0e\x32\t.FileType\x12\x0c\n\x04name\x18\x02 \x01(\t\x12\x0c\n\x04size\x18\x03 \x01(\x03\x12\x12\n\npermission\x18\x04 \x01(\x05\x12\x10\n\x08\x62lob_ids\x18\x05 \x03(\t\x12\x13\n\x0bsigned_urls\x18\x64 \x03(\t\x12\x13\n\x0b\x62lob_offset\x18\x06 \x01(\x05\x12\x11\n\tblob_size\x18\x07 \x01(\x05\x12\x0b\n\x03md5\x18\x08 \x01(\x0c\x12\x34\n\x15\x63ompression_algorithm\x18\t \x01(\x0e\x32\x15.CompressionAlgorithm\x12\x17\n\x0f\x66rom_file_index\x18\n \x01(\x05\x12\x15\n\rto_file_index\x18\x0b \x01(\x05\"9\n\rMetaBlobIndex\x12\x0f\n\x07\x62lob_id\x18\x01 \x01(\t\x12\x17\n\x0flast_file_index\x18\x02 \x01(\x05\"Y\n\x08MetaBlob\x12\x14\n\x05\x66iles\x18\x01 \x03(\x0b\x32\x05.File\x12)\n\x11meta_blob_indexes\x18\x02 \x03(\x0b\x32\x0e.MetaBlobIndex\x12\x0c\n\x04\x64\x61ta\x18\x03 \x01(\x0c*_\n\x14\x43ompressionAlgorithm\x12(\n$COMPRESSION_ALGORITHM_NO_COMPRESSION\x10\x00\x12\x1d\n\x19\x43OMPRESSION_ALGORITHM_LZ4\x10\x01*N\n\x08\x46ileType\x12\x15\n\x11\x46ILE_TYPE_REGULAR\x10\x00\x12\x12\n\x0e\x46ILE_TYPE_HUGE\x10\x01\x12\x17\n\x13\x46ILE_TYPE_DIRECTORY\x10\x02\x42!\n\x1f\x61i.starwhale.mlops.domain.modelb\x06proto3')

_COMPRESSIONALGORITHM = DESCRIPTOR.enum_types_by_name['CompressionAlgorithm']
CompressionAlgorithm = enum_type_wrapper.EnumTypeWrapper(_COMPRESSIONALGORITHM)
_FILETYPE = DESCRIPTOR.enum_types_by_name['FileType']
FileType = enum_type_wrapper.EnumTypeWrapper(_FILETYPE)
COMPRESSION_ALGORITHM_NO_COMPRESSION = 0
COMPRESSION_ALGORITHM_LZ4 = 1
FILE_TYPE_REGULAR = 0
FILE_TYPE_HUGE = 1
FILE_TYPE_DIRECTORY = 2


_FILE = DESCRIPTOR.message_types_by_name['File']
_METABLOBINDEX = DESCRIPTOR.message_types_by_name['MetaBlobIndex']
_METABLOB = DESCRIPTOR.message_types_by_name['MetaBlob']
File = _reflection.GeneratedProtocolMessageType('File', (_message.Message,), {
  'DESCRIPTOR' : _FILE,
  '__module__' : 'model_package_storage_pb2'
  # @@protoc_insertion_point(class_scope:File)
  })
_sym_db.RegisterMessage(File)

MetaBlobIndex = _reflection.GeneratedProtocolMessageType('MetaBlobIndex', (_message.Message,), {
  'DESCRIPTOR' : _METABLOBINDEX,
  '__module__' : 'model_package_storage_pb2'
  # @@protoc_insertion_point(class_scope:MetaBlobIndex)
  })
_sym_db.RegisterMessage(MetaBlobIndex)

MetaBlob = _reflection.GeneratedProtocolMessageType('MetaBlob', (_message.Message,), {
  'DESCRIPTOR' : _METABLOB,
  '__module__' : 'model_package_storage_pb2'
  # @@protoc_insertion_point(class_scope:MetaBlob)
  })
_sym_db.RegisterMessage(MetaBlob)

if _descriptor._USE_C_DESCRIPTORS == False:

  DESCRIPTOR._options = None
  DESCRIPTOR._serialized_options = b'\n\037ai.starwhale.mlops.domain.model'
  _COMPRESSIONALGORITHM._serialized_start=457
  _COMPRESSIONALGORITHM._serialized_end=552
  _FILETYPE._serialized_start=554
  _FILETYPE._serialized_end=632
  _FILE._serialized_start=32
  _FILE._serialized_end=305
  _METABLOBINDEX._serialized_start=307
  _METABLOBINDEX._serialized_end=364
  _METABLOB._serialized_start=366
  _METABLOB._serialized_end=455
# @@protoc_insertion_point(module_scope)