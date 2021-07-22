enum ChannelStatus { unavailable, available }

class AllChannelStatus {
  final bool needChannel;
  final bool channelsCreated;

  /// channel id to status
  final Map<String, ChannelStatus> status;

  AllChannelStatus({this.needChannel = false, this.channelsCreated = false, this.status});

  factory AllChannelStatus.fromMap(Map<dynamic, dynamic> map) {
    return AllChannelStatus(
      needChannel: map['needChannel'],
      channelsCreated: map['channelsCreated'],
      status: (map['status'] as Map<dynamic, dynamic>)
          .map((key, value) => MapEntry(key.toString(), _getChannelStatus(value))),
    );
  }

  @override
  String toString() {
    return 'AllChannelStatus{needChannel: $needChannel, channelsCreated: $channelsCreated, status: $status}';
  }
}

/// Gets the ChannelStatus from the channel Method
///
/// Given a [String] status from the method channel, it returns a
/// [ChannelStatus]
ChannelStatus _getChannelStatus(String status) {
  switch (status) {
    case "available":
      return ChannelStatus.available;
    default:
      return ChannelStatus.unavailable;
  }
}
