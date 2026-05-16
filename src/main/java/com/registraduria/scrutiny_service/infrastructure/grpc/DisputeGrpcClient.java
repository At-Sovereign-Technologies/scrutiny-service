package com.registraduria.scrutiny_service.infrastructure.grpc;

import java.util.List;

import org.springframework.stereotype.Component;

import com.registraduria.scrutinyservice.grpc.DisputeGrpcServiceGrpc;
import com.registraduria.scrutinyservice.grpc.EmptyRequest;
import com.registraduria.scrutinyservice.grpc.QuarantinedMesa;
import com.registraduria.scrutinyservice.grpc.QuarantinedMesaList;

import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Component
@RequiredArgsConstructor
public class DisputeGrpcClient {

    @GrpcClient("dispute-service")
    private DisputeGrpcServiceGrpc
            .DisputeGrpcServiceBlockingStub stub;

    public List<String> getQuarantinedMesaCodes() {

        QuarantinedMesaList response =
                stub.getQuarantinedMesaCodes(
                        EmptyRequest.newBuilder()
                                .build()
                );

        return response.getMesasList()
                .stream()
                .map(QuarantinedMesa::getMesaCode)
                .toList();
    }
}